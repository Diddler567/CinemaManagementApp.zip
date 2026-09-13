package com.example.backend.security;

import com.example.backend.audit.AuditService;
import com.example.backend.entities.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(5)
public class RateLimitFilter extends OncePerRequestFilter {

    private final AntPathMatcher matcher = new AntPathMatcher();
    private final AuditService auditService;

    public RateLimitFilter(AuditService auditService) {
        this.auditService = auditService;
    }

    private static class Rule {
        private final int maxRequests;
        private final int windowSeconds;

        private Rule(int maxRequests, int windowSeconds) {
            this.maxRequests = maxRequests;
            this.windowSeconds = windowSeconds;
        }

        public int getMaxRequests() { return maxRequests; }
        public int getWindowSeconds() { return windowSeconds; }
    }

    private final List<Map.Entry<String, Rule>> rules = Arrays.asList(
            new AbstractMap.SimpleEntry<>("/auth/login", new Rule(5, 60)),
            new AbstractMap.SimpleEntry<>("/auth/register", new Rule(3, 60)),
            new AbstractMap.SimpleEntry<>("/screenings/search", new Rule(60, 60)),
            new AbstractMap.SimpleEntry<>("/programs", new Rule(60, 60)),
            new AbstractMap.SimpleEntry<>("/screenings/*/submit", new Rule(10, 60)),
            new AbstractMap.SimpleEntry<>("/screenings/*/final-submit", new Rule(10, 60)),
            new AbstractMap.SimpleEntry<>("/programs/*/state", new Rule(20, 60)),
            new AbstractMap.SimpleEntry<>("/programs/*/roles", new Rule(20, 60))
    );

    private final Rule defaultRule = new Rule(200, 60);
    private final ConcurrentHashMap<String, ArrayDeque<Long>> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        if ("OPTIONS".equalsIgnoreCase(method)) {
            filterChain.doFilter(request, response);
            return;
        }

        Rule rule = resolveRule(path);

        String identity = resolveIdentityKey(request);
        String key = identity + "|" + pathGroupKey(path);

        long now = System.currentTimeMillis();
        long windowMs = rule.getWindowSeconds() * 1000L;

        ArrayDeque<Long> deque = buckets.computeIfAbsent(key, k -> new ArrayDeque<Long>());

        synchronized (deque) {
            while (!deque.isEmpty() && (now - deque.peekFirst()) > windowMs) {
                deque.pollFirst();
            }

            if (deque.size() >= rule.getMaxRequests()) {
                long oldest = deque.peekFirst();
                long waitMs = windowMs - (now - oldest);
                int retryAfterSeconds = (int) Math.max(1, (waitMs + 999) / 1000);

                write429(response, request, retryAfterSeconds, rule);
                return;
            }

            deque.addLast(now);
        }

        filterChain.doFilter(request, response);
    }

    private Rule resolveRule(String path) {
        for (Map.Entry<String, Rule> e : rules) {
            if (matcher.match(e.getKey(), path)) {
                return e.getValue();
            }
        }
        return defaultRule;
    }

    private String resolveIdentityKey(HttpServletRequest request) {
        Object obj = request.getAttribute(AuthTokenFilter.AUTH_USER_ATTR);
        if (obj instanceof User) {
            User u = (User) obj;
            return "USER:" + u.getUserID();
        }
        return "IP:" + getClientIp(request);
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String pathGroupKey(String path) {
        return path
                .replaceAll("/screenings/\\d+/submit", "/screenings/*/submit")
                .replaceAll("/screenings/\\d+/final-submit", "/screenings/*/final-submit")
                .replaceAll("/programs/\\d+/state", "/programs/*/state")
                .replaceAll("/programs/\\d+/roles", "/programs/*/roles")
                .replaceAll("/users/\\d+", "/users/*");
    }

    private void write429(HttpServletResponse response,
                        HttpServletRequest request,
                        int retryAfterSeconds,
                        Rule rule) throws IOException {

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));

        auditService.logAnonymous("RATE_LIMIT", "ENDPOINT", null,
                "path=" + request.getRequestURI() + " retryAfter=" + retryAfterSeconds);

        String path = request.getRequestURI() == null ? "" : request.getRequestURI().replace("\"", "\\\"");
        String json =
                "{"
                        + "\"timestamp\":\"" + Instant.now().toString() + "\","
                        + "\"status\":" + HttpStatus.TOO_MANY_REQUESTS.value() + ","
                        + "\"error\":\"" + HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase() + "\","
                        + "\"message\":\"Rate limit exceeded\","
                        + "\"path\":\"" + path + "\","
                        + "\"details\":{"
                        + "\"limit\":" + rule.getMaxRequests() + ","
                        + "\"windowSeconds\":" + rule.getWindowSeconds() + ","
                        + "\"retryAfterSeconds\":" + retryAfterSeconds
                        + "}"
                        + "}";

        response.getWriter().write(json);
    }
}
