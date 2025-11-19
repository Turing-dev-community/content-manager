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
    private final RateLimitConfigService configService;

    public RateLimitService(
            @Value("${rate-limit.capacity:100}") long capacity,
            @Value("${rate-limit.refillTokens:100}") long refillTokens,
            @Value("${rate-limit.refillIntervalMillis:60000}") long refillIntervalMillis, RateLimitConfigService configService
    ) {
        this.capacity = capacity;
        this.refillTokens = refillTokens;
        this.refillIntervalMillis = refillIntervalMillis;
        this.configService = configService;
    }

    public TokenBucket getBucketForKey(String key) {
        return buckets.computeIfAbsent(key, k -> new TokenBucket(capacity, refillTokens, refillIntervalMillis));
    }

    public TokenBucket getBucketForKeyWithParams(String composedKey, long cap, long refillTokens, long refillIntervalMillis) {
        return buckets.computeIfAbsent(composedKey, k -> new TokenBucket(cap, refillTokens, refillIntervalMillis));
    }

    public long getDefaultCapacity() { return capacity; }
    public long getDefaultRefillTokens() { return refillTokens; }
    public long getDefaultRefillIntervalMillis() { return refillIntervalMillis; }

    public RateLimitConfigService getConfigService() {
        return configService;
    }
}
