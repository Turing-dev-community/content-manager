package com.dehold.contentmanager.ratelimiter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitService {
    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    private final long capacity;
    private final long refillTokens;
    private final long refillIntervalMillis;

    public RateLimitService(
            @Value("${rate-limit.capacity:100}") long capacity,
            @Value("${rate-limit.refillTokens:100}") long refillTokens,
            @Value("${rate-limit.refillIntervalMillis:60000}") long refillIntervalMillis
    ) {
        this.capacity = capacity;
        this.refillTokens = refillTokens;
        this.refillIntervalMillis = refillIntervalMillis;
    }

    public TokenBucket getBucketForKey(String key) {
        return buckets.computeIfAbsent(key, k -> new TokenBucket(capacity, refillTokens, refillIntervalMillis));
    }
}
