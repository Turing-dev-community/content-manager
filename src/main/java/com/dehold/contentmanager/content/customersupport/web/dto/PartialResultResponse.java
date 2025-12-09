package com.dehold.contentmanager.content.customersupport.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.*;

/**
 * Response object for partial results when some items fail to fetch
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PartialResultResponse<T> {
    private List<T> successfulResults;
    private Map<UUID, FailedIdInfo> failedIds;
    private RetryInfo retryInfo;
    private boolean isPartial;
    private Instant timestamp;
    
    public PartialResultResponse() {
        this.timestamp = Instant.now();
        this.isPartial = false;
    }
    
    public PartialResultResponse(List<T> successfulResults) {
        this();
        this.successfulResults = successfulResults;
    }
    
    public PartialResultResponse(List<T> successfulResults, Map<UUID, FailedIdInfo> failedIds) {
        this(successfulResults);
        this.failedIds = failedIds;
        this.isPartial = !failedIds.isEmpty();
    }
    
    public PartialResultResponse(List<T> successfulResults, Map<UUID, FailedIdInfo> failedIds, 
                                RetryInfo retryInfo) {
        this(successfulResults, failedIds);
        this.retryInfo = retryInfo;
    }
    
    // Getters and setters
    
    public List<T> getSuccessfulResults() {
        return successfulResults;
    }
    
    public void setSuccessfulResults(List<T> successfulResults) {
        this.successfulResults = successfulResults;
    }
    
    public Map<UUID, FailedIdInfo> getFailedIds() {
        return failedIds;
    }
    
    public void setFailedIds(Map<UUID, FailedIdInfo> failedIds) {
        this.failedIds = failedIds;
    }
    
    public RetryInfo getRetryInfo() {
        return retryInfo;
    }
    
    public void setRetryInfo(RetryInfo retryInfo) {
        this.retryInfo = retryInfo;
    }
    
    public boolean isPartial() {
        return isPartial;
    }
    
    public void setPartial(boolean partial) {
        isPartial = partial;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
    
    public int getSuccessCount() {
        return successfulResults != null ? successfulResults.size() : 0;
    }
    
    public int getFailureCount() {
        return failedIds != null ? failedIds.size() : 0;
    }
    
    /**
     * Information about a failed ID fetch
     */
    public static class FailedIdInfo {
        private UUID id;
        private String failureReason;
        private int remainingRetries;
        private Instant failureTime;
        
        public FailedIdInfo() {}
        
        public FailedIdInfo(UUID id, String failureReason, int remainingRetries) {
            this.id = id;
            this.failureReason = failureReason;
            this.remainingRetries = remainingRetries;
            this.failureTime = Instant.now();
        }
        
        // Getters and setters
        
        public UUID getId() {
            return id;
        }
        
        public void setId(UUID id) {
            this.id = id;
        }
        
        public String getFailureReason() {
            return failureReason;
        }
        
        public void setFailureReason(String failureReason) {
            this.failureReason = failureReason;
        }
        
        public int getRemainingRetries() {
            return remainingRetries;
        }
        
        public void setRemainingRetries(int remainingRetries) {
            this.remainingRetries = remainingRetries;
        }
        
        public Instant getFailureTime() {
            return failureTime;
        }
        
        public void setFailureTime(Instant failureTime) {
            this.failureTime = failureTime;
        }
    }
    
    /**
     * Retry information and recommendations
     */
    public static class RetryInfo {
        private long retryAfterMs;
        private String retryStrategy;
        private int maxRetries;
        private int currentAttempt;
        
        public RetryInfo() {}
        
        public RetryInfo(long retryAfterMs, String retryStrategy, int maxRetries, int currentAttempt) {
            this.retryAfterMs = retryAfterMs;
            this.retryStrategy = retryStrategy;
            this.maxRetries = maxRetries;
            this.currentAttempt = currentAttempt;
        }
        
        // Getters and setters
        
        public long getRetryAfterMs() {
            return retryAfterMs;
        }
        
        public void setRetryAfterMs(long retryAfterMs) {
            this.retryAfterMs = retryAfterMs;
        }
        
        public String getRetryStrategy() {
            return retryStrategy;
        }
        
        public void setRetryStrategy(String retryStrategy) {
            this.retryStrategy = retryStrategy;
        }
        
        public int getMaxRetries() {
            return maxRetries;
        }
        
        public void setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
        }
        
        public int getCurrentAttempt() {
            return currentAttempt;
        }
        
        public void setCurrentAttempt(int currentAttempt) {
            this.currentAttempt = currentAttempt;
        }
    }
}
