package com.example.backend.security;

import com.example.backend.audit.AuditService;
import com.example.backend.dto.ApiErrorResponse;
import com.example.backend.entities.AuthenticationToken;
import com.example.backend.entities.PermanentRole;
import com.example.backend.entities.User;
import com.example.backend.repository.AuthenticationTokenRepository;
import com.example.backend.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class AuthTokenFilter extends OncePerRequestFilter {

    public static final String AUTH_USER_ATTR = "AUTH_USER";

    //ERROR CODES (PDF REQUIREMENT)
    private static final String CODE_TOKEN_MISSING = "TOKEN_MISSING";
    private static final String CODE_TOKEN_INVALID = "TOKEN_INVALID";
    private static final String CODE_TOKEN_EXPIRED = "TOKEN_EXPIRED";
    private static final String CODE_TOKEN_NOT_OWNER = "TOKEN_NOT_OWNER";

    //EXTRA (USEFUL)
    private static final String CODE_USER_INACTIVE = "USER_INACTIVE";

    //PUBLIC-ISH GET ENDPOINTS THAT MUST WORK FOR VISITOR (PDF)
    private static final Pattern PUBLIC_SCREENING_ID = Pattern.compile("^/screenings/(\\d+)$");

    private final AuthenticationTokenRepository tokenRepository;
    private final UserService userService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public AuthTokenFilter(AuthenticationTokenRepository tokenRepository,
                        UserService userService,
                        AuditService auditService,
                        ObjectMapper objectMapper) {
        this.tokenRepository = tokenRepository;
        this.userService = userService;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    private void writeJsonError(HttpServletRequest request,
                                HttpServletResponse response,
                                HttpStatus status,
                                String message,
                                String code) throws IOException {

        response.setStatus(status.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                Map.of("code", code)
        );

        objectMapper.writeValue(response.getOutputStream(), body);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            writeJsonError(request, response,
                    HttpStatus.UNAUTHORIZED,
                    "Missing or invalid Authorization header",
                    CODE_TOKEN_MISSING);
            return;
        }

        String tokenValue = authHeader.substring(7);

        Optional<AuthenticationToken> tokenOpt =
                tokenRepository.findByTokenValueAndIsValidTrue(tokenValue);

        //INVALID
        if (tokenOpt.isEmpty()) {
            writeJsonError(request, response,
                    HttpStatus.UNAUTHORIZED,
                    "Invalid token",
                    CODE_TOKEN_INVALID);
            return;
        }

        AuthenticationToken token = tokenOpt.get();

        //EXPIRED
        if (token.getExpiresAt() != null && token.getExpiresAt().isBefore(Instant.now())) {
            token.setValid(false);
            tokenRepository.save(token);

            writeJsonError(request, response,
                    HttpStatus.UNAUTHORIZED,
                    "Token expired",
                    CODE_TOKEN_EXPIRED);
            return;
        }

        User user = token.getUser();
        if (user == null || !user.isActive()) {
            writeJsonError(request, response,
                    HttpStatus.UNAUTHORIZED,
                    "User inactive",
                    CODE_USER_INACTIVE);
            return;
        }

        //TOKEN USED AS OTHER USER => DEACTIVATE BOTH
        String path = request.getRequestURI();
        if (path != null && path.startsWith("/users/") && !path.startsWith("/users/me")) {

            Matcher m = Pattern.compile("^/users/(\\d+)(/.*)?$").matcher(path);
            if (m.matches()) {
                Long targetId = Long.parseLong(m.group(1));

                boolean isAdmin = (user.getPermanentRole() == PermanentRole.ADMIN);
                boolean isSelf = user.getUserID().equals(targetId);

                //NOT OWNER (AND ALSO DEACTIVATE BOTH, PER PDF)
                if (!isAdmin && !isSelf) {
                    User target = userService.getUserOrThrow(targetId);

                    userService.deactivateBoth(user, target);

                    auditService.log(user, "TOKEN_USED_AS_OTHER_USER", "USER", targetId,
                            "Both users deactivated");

                    writeJsonError(request, response,
                            HttpStatus.FORBIDDEN,
                            "Token does not belong to the requester. Both accounts deactivated.",
                            CODE_TOKEN_NOT_OWNER);
                    return;
                }
            }
        }

        request.setAttribute(AUTH_USER_ATTR, user);
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null) return false;
        
        //ΑΠΑΡΑΊΤΗΤΟ ΓΙΑ CORS PREFLIGHT
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;


        //ΑΦΉΝΟΥΜΕ ΤΗΝ H2 CONSOLE ΧΩΡΊΣ TOKEN
        if (path.equals("/h2") || path.startsWith("/h2/")) return true;

        //AUTH ENDPOINTS
        if (path.startsWith("/auth/")) return true;

        // FULLY PUBLIC ENDPOINTS
        if (path.startsWith("/public/")) return true;

        
        
        if ("GET".equalsIgnoreCase(request.getMethod())) {
            String authHeader = request.getHeader("Authorization");
            boolean hasBearer = authHeader != null && authHeader.startsWith("Bearer ");

            if (!hasBearer) {
                if (path.equals("/screenings/search")) return true;
                if (PUBLIC_SCREENING_ID.matcher(path).matches()) return true;
            }
        }

        return false;
    }

}
