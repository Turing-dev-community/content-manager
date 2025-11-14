package com.dehold.contentmanager.ratelimiter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.security.Principal;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitService rateLimitService;

    public RateLimitInterceptor(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    private String extractKey(HttpServletRequest request) {
        // 1) If user is authenticated, prefer a stable identifier from Principal
        Principal principal = request.getUserPrincipal();
        if (principal != null && principal.getName() != null) {
            return "user:" + principal.getName();
        }
        // 2) Try custom header (if you use API keys or X-User-Id in tests)
        String headerUser = request.getHeader("X-User-Id");
        if (headerUser != null && !headerUser.isBlank()) {
            return "userIdHeader:" + headerUser;
        }
        // 3) fallback to IP address — note: behind proxies you may want X-Forwarded-For
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        }
        return "ip:" + ip;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        // Apply rate limiting only to API paths; you can further refine by path if needed
        String path = request.getRequestURI();
        if (!path.startsWith("/api/")) {
            return true;
        }

        String key = extractKey(request);
        TokenBucket bucket = rateLimitService.getBucketForKey(key);

        boolean allowed = bucket.tryConsume(1);
        if (!allowed) {
            response.setStatus(429);
            response.setHeader("Retry-After", "60"); // best-effort hint
            response.getWriter().write("{\"error\":\"Too Many Requests\"}");
            return false;
        }
        return true;
    }
}
