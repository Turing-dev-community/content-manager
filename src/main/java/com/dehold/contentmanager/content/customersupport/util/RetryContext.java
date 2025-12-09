package com.dehold.contentmanager.content.customersupport.util;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Context for tracking retry attempts and backoff strategy
 */
public class RetryContext {
    private int attemptCount;
    private int maxAttempts;
    private long initialDelayMs;
    private double backoffMultiplier;
    private Instant lastAttemptTime;
    private UUID targetId;
    private Exception lastException;
    
    public RetryContext(int maxAttempts, long initialDelayMs, double backoffMultiplier) {
        this.maxAttempts = maxAttempts;
        this.initialDelayMs = initialDelayMs;
        this.backoffMultiplier = backoffMultiplier;
        this.attemptCount = 0;
        this.lastAttemptTime = Instant.now();
    }
    
    public RetryContext(int maxAttempts, long initialDelayMs, double backoffMultiplier, UUID targetId) {
        this(maxAttempts, initialDelayMs, backoffMultiplier);
        this.targetId = targetId;
    }
    
    public void recordAttempt(Exception exception) {
        this.attemptCount++;
        this.lastAttemptTime = Instant.now();
        this.lastException = exception;
    }
    
    public boolean canRetry() {
        return attemptCount < maxAttempts;
    }
    
    public long getNextBackoffDelayMs() {
        return (long) (initialDelayMs * Math.pow(backoffMultiplier, attemptCount - 1));
    }
    
    public int getAttemptCount() {
        return attemptCount;
    }
    
    public int getMaxAttempts() {
        return maxAttempts;
    }
    
    public Instant getLastAttemptTime() {
        return lastAttemptTime;
    }
    
    public UUID getTargetId() {
        return targetId;
    }
    
    public Exception getLastException() {
        return lastException;
    }
    
    public Duration getTimeSinceLastAttempt() {
        return Duration.between(lastAttemptTime, Instant.now());
    }
}
