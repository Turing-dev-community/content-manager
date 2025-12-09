package com.dehold.contentmanager.content.customersupport.util;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks IDs that failed during fetch operations and their retry info
 */
public class FailedIdTracker {
    private final Map<UUID, FailureInfo> failedIds;
    private final int maxTrackedIds;
    
    public FailedIdTracker() {
        this(1000); // default max tracked IDs
    }
    
    public FailedIdTracker(int maxTrackedIds) {
        this.failedIds = new ConcurrentHashMap<>();
        this.maxTrackedIds = maxTrackedIds;
    }
    
    public void recordFailure(UUID id, String reason, int remainingRetries) {
        if (failedIds.size() >= maxTrackedIds) {
            // Remove oldest entry if at capacity
            failedIds.entrySet().stream()
                    .min(Comparator.comparing(e -> e.getValue().failureTime))
                    .ifPresent(e -> failedIds.remove(e.getKey()));
        }
        
        failedIds.put(id, new FailureInfo(reason, remainingRetries, Instant.now()));
    }
    
    public void removeFailure(UUID id) {
        failedIds.remove(id);
    }
    
    public boolean isTracked(UUID id) {
        return failedIds.containsKey(id);
    }
    
    public FailureInfo getFailureInfo(UUID id) {
        return failedIds.get(id);
    }
    
    public Set<UUID> getFailedIds() {
        return new HashSet<>(failedIds.keySet());
    }
    
    public Map<UUID, FailureInfo> getAllFailures() {
        return new HashMap<>(failedIds);
    }
    
    public int getFailureCount() {
        return failedIds.size();
    }
    
    public void clear() {
        failedIds.clear();
    }
    
    /**
     * Inner class to store failure information
     */
    public static class FailureInfo {
        public final String reason;
        public final int remainingRetries;
        public final Instant failureTime;
        
        public FailureInfo(String reason, int remainingRetries, Instant failureTime) {
            this.reason = reason;
            this.remainingRetries = remainingRetries;
            this.failureTime = failureTime;
        }
    }
}
