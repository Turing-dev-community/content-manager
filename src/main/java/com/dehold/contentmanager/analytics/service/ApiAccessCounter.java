package com.dehold.contentmanager.analytics.service;

import com.dehold.contentmanager.analytics.model.ApiAccessLog;
import com.dehold.contentmanager.analytics.repository.ApiAccessLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Component
public class ApiAccessCounter implements HandlerInterceptor {
    private final ApiAccessLogRepository apiAccessLogRepository;

    public ApiAccessCounter(ApiAccessLogRepository apiAccessLogRepository) {
        this.apiAccessLogRepository = apiAccessLogRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {

        // The pattern matcher makes sure that path variables are not counted separately
        // e.g. /api/blogposts/123 and /api/blogposts/456 will result in the same pattern /api/blogposts/{id}
        String pattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);

        if (pattern != null) {
            ApiAccessLog accessLog = new ApiAccessLog(UUID.randomUUID(), pattern, Instant.now());
            apiAccessLogRepository.save(accessLog);
        }

        return true;
    }
}

