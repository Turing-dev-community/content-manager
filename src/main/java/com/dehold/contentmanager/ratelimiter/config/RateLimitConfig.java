package com.dehold.contentmanager.ratelimiter.config;

import java.time.Instant;
import java.util.UUID;

public class RateLimitConfig {
    private UUID id;
    private String pathPattern;
    private long capacity;
    private long refillTokens;
    private long refillIntervalMillis;
    private Instant createdAt;
    private Instant updatedAt;

    public RateLimitConfig() {}

    public RateLimitConfig(UUID id, String pathPattern, long capacity, long refillTokens, long refillIntervalMillis, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.pathPattern = pathPattern;
        this.capacity = capacity;
        this.refillTokens = refillTokens;
        this.refillIntervalMillis = refillIntervalMillis;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getPathPattern() {
        return pathPattern;
    }

    public void setPathPattern(String pathPattern) {
        this.pathPattern = pathPattern;
    }

    public long getCapacity() {
        return capacity;
    }

    public void setCapacity(long capacity) {
        this.capacity = capacity;
    }

    public long getRefillTokens() {
        return refillTokens;
    }

    public void setRefillTokens(long refillTokens) {
        this.refillTokens = refillTokens;
    }

    public long getRefillIntervalMillis() {
        return refillIntervalMillis;
    }

    public void setRefillIntervalMillis(long refillIntervalMillis) {
        this.refillIntervalMillis = refillIntervalMillis;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
