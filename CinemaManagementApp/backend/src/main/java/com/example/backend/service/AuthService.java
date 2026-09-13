package com.example.backend.service;

import com.example.backend.dto.LoginResponse;
import com.example.backend.entities.AuthenticationToken;
import com.example.backend.entities.User;
import com.example.backend.repository.AuthenticationTokenRepository;
import com.example.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.audit.AuditService;


import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final AuthenticationTokenRepository tokenRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AuditService auditService;


    private final Duration tokenTtl;

    public AuthService(UserRepository userRepository,
                AuthenticationTokenRepository tokenRepository,
                BCryptPasswordEncoder passwordEncoder,
                AuditService auditService,
                @Value("${app.auth.token-ttl-seconds:3600}") long tokenTtlSeconds) {

        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenTtl = Duration.ofSeconds(tokenTtlSeconds);
        this.auditService = auditService;

    }

    @Transactional(noRollbackFor = RuntimeException.class)
    public LoginResponse login(String username, String password) {

        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            auditService.logAnonymous("AUTH_LOGIN_FAILED", "USER", null,
                "username=" + username + " reason=not_found");
            throw new RuntimeException("Invalid credentials");
        }

        if (!user.isActive()) {
            auditService.log(user, "AUTH_LOGIN_BLOCKED_INACTIVE", "USER", user.getUserID(),
                "username=" + username);
            throw new RuntimeException("User is inactive");
        }

        boolean ok = passwordEncoder.matches(password, user.getPasswordHash());

        if (!ok) {
            user.setFailedAuthCount(user.getFailedAuthCount() + 1);

            boolean deactivated = false;
            if (user.getFailedAuthCount() >= 3) {
                user.setActive(false);
                deactivated = true;
            }

            userRepository.save(user);

            auditService.log(user, "AUTH_LOGIN_FAILED", "USER", user.getUserID(),
                "failedAuthCount=" + user.getFailedAuthCount());

            if (deactivated) {
                auditService.log(user, "USER_DEACTIVATED_AUTH_FAIL", "USER", user.getUserID(),
                    "failedAuthCount=" + user.getFailedAuthCount());
            }

            throw new RuntimeException("Invalid credentials");
        }

        user.setFailedAuthCount(0);
        userRepository.save(user);

        //INVALIDATE PREVIOUS TOKEN(S)
        tokenRepository.invalidateAllForUser(user);

        //ISSUE NEW TOKEN
        String tokenValue = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(tokenTtl);

        tokenRepository.save(new AuthenticationToken(tokenValue, user, expiresAt));

        auditService.log(user, "AUTH_LOGIN_SUCCESS", "USER", user.getUserID(),
            "tokenIssued=true ttlSeconds=" + tokenTtl.toSeconds());

        return new LoginResponse(tokenValue, user.getUserID(), user.getPermanentRole().name());
    }


    @Transactional
    public void logout(String tokenValue) {
        var tok = tokenRepository.findByTokenValue(tokenValue).orElse(null);
        int changed = tokenRepository.invalidateByTokenValue(tokenValue);

        if (tok != null && tok.getUser() != null) {
        auditService.log(tok.getUser(), "AUTH_LOGOUT", "USER", tok.getUser().getUserID(),
                "tokensInvalidated=" + changed);
        } else {
            auditService.logAnonymous("AUTH_LOGOUT", "TOKEN", null,
                "tokensInvalidated=" + changed + " tokenFound=false");
        }
    }

}


