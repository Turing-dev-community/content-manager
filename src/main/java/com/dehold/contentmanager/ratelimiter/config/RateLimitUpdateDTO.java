package com.dehold.contentmanager.ratelimiter.config;

public class RateLimitUpdateDTO {
    private String pathPattern;
    private long capacity;
    private long refillTokens;
    private long refillIntervalMillis;

    // Default constructor
    public RateLimitUpdateDTO() {}

    // Constructor for testing
    public RateLimitUpdateDTO(String pathPattern, long capacity, long refillTokens, long refillIntervalMillis) {
        this.pathPattern = pathPattern;
        this.capacity = capacity;
        this.refillTokens = refillTokens;
        this.refillIntervalMillis = refillIntervalMillis;
    }

    // --- Getters and Setters ---

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
}