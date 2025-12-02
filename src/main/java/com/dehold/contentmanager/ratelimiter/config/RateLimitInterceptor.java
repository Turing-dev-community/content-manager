package com.dehold.contentmanager.ratelimiter.config;

import com.dehold.contentmanager.ratelimiter.service.RateLimitConfigService;
import com.dehold.contentmanager.ratelimiter.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.security.Principal;
import java.util.Optional;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitService rateLimitService;

    public RateLimitInterceptor(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    private String extractKey(HttpServletRequest request) {
        Principal principal = request.getUserPrincipal();
        if (principal != null && principal.getName() != null) {
            return "user:" + principal.getName();
        }
        String headerUser = request.getHeader("X-User-Id");
        if (headerUser != null && !headerUser.isBlank()) {
            return "userIdHeader:" + headerUser;
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        }
        return "ip:" + ip;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String path = request.getRequestURI();
        if (!path.startsWith("/api/")) {
            return true;
        }

        String userKey = extractKey(request);

        // If we have config service available, try to find best matching config
        RateLimitConfigService cfgService = rateLimitService.getConfigService();
        if (cfgService != null) {
            Optional<RateLimitConfig> maybe = cfgService.findBestMatchForPath(path);
            if (maybe.isPresent()) {
                RateLimitConfig cfg = maybe.get();
                String composedKey = "cfg:" + cfg.getPathPattern() + ":key:" + userKey;
                TokenBucket bucket = rateLimitService.getBucketForKeyWithParams(
                        composedKey,
                        cfg.getCapacity(),
                        cfg.getRefillTokens(),
                        cfg.getRefillIntervalMillis()
                );

                if (!bucket.tryConsume(1)) {
                    response.setStatus(429);
                    response.setHeader("Retry-After", String.valueOf(cfg.getRefillIntervalMillis() / 1000));
                    response.getWriter().write("{\"error\":\"Too Many Requests\"}");
                    return false;
                }
                return true;
            }
        }

        // fallback to global defaults
        TokenBucket bucket = rateLimitService.getBucketForKey("global:" + userKey);
        boolean allowed = bucket.tryConsume(1);
        if (!allowed) {
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(rateLimitService.getDefaultRefillIntervalMillis() / 1000));
            response.getWriter().write("{\"error\":\"Too Many Requests\"}");
            return false;
        }
        return true;
    }
}
