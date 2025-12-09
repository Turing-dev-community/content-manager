package com.dehold.contentmanager.content.customersupport.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FailedIdTracker Tests")
class FailedIdTrackerTest {

    private FailedIdTracker tracker;

    @BeforeEach
    void setUp() {
        tracker = new FailedIdTracker();
    }

    @Test
    @DisplayName("Should initialize with empty failed IDs")
    void testInitialization() {
        assertTrue(tracker.getFailedIds().isEmpty());
        assertEquals(0, tracker.getFailureCount());
    }

    @Test
    @DisplayName("Should record failure with all required info")
    void testRecordFailure() {
        UUID id = UUID.randomUUID();
        String reason = "Database connection timeout";
        int remainingRetries = 2;
        
        tracker.recordFailure(id, reason, remainingRetries);
        
        assertTrue(tracker.isTracked(id));
        FailedIdTracker.FailureInfo info = tracker.getFailureInfo(id);
        assertNotNull(info);
        assertEquals(reason, info.reason);
        assertEquals(remainingRetries, info.remainingRetries);
        assertNotNull(info.failureTime);
    }

    @Test
    @DisplayName("Should track multiple failed IDs")
    void testTrackMultipleFailures() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();
        
        tracker.recordFailure(id1, "Error 1", 1);
        tracker.recordFailure(id2, "Error 2", 2);
        tracker.recordFailure(id3, "Error 3", 0);
        
        assertEquals(3, tracker.getFailureCount());
        assertTrue(tracker.isTracked(id1));
        assertTrue(tracker.isTracked(id2));
        assertTrue(tracker.isTracked(id3));
    }

    @Test
    @DisplayName("Should remove failure from tracking")
    void testRemoveFailure() {
        UUID id = UUID.randomUUID();
        tracker.recordFailure(id, "Test error", 1);
        
        assertTrue(tracker.isTracked(id));
        tracker.removeFailure(id);
        assertFalse(tracker.isTracked(id));
    }

    @Test
    @DisplayName("Should return false for untracked IDs")
    void testUnTrackedId() {
        UUID id = UUID.randomUUID();
        assertFalse(tracker.isTracked(id));
    }

    @Test
    @DisplayName("Should return null for untracked ID failure info")
    void testGetFailureInfoForUntrackedId() {
        UUID id = UUID.randomUUID();
        assertNull(tracker.getFailureInfo(id));
    }

    @Test
    @DisplayName("Should return all failed IDs as set")
    void testGetFailedIds() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();
        
        tracker.recordFailure(id1, "Error 1", 1);
        tracker.recordFailure(id2, "Error 2", 2);
        tracker.recordFailure(id3, "Error 3", 0);
        
        Set<UUID> failedIds = tracker.getFailedIds();
        assertEquals(3, failedIds.size());
        assertTrue(failedIds.contains(id1));
        assertTrue(failedIds.contains(id2));
        assertTrue(failedIds.contains(id3));
    }

    @Test
    @DisplayName("Should return all failures as map")
    void testGetAllFailures() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        
        tracker.recordFailure(id1, "Error 1", 1);
        tracker.recordFailure(id2, "Error 2", 2);
        
        Map<UUID, FailedIdTracker.FailureInfo> allFailures = tracker.getAllFailures();
        
        assertEquals(2, allFailures.size());
        assertTrue(allFailures.containsKey(id1));
        assertTrue(allFailures.containsKey(id2));
        assertEquals("Error 1", allFailures.get(id1).reason);
        assertEquals("Error 2", allFailures.get(id2).reason);
    }

    @Test
    @DisplayName("Should clear all tracked failures")
    void testClear() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        
        tracker.recordFailure(id1, "Error 1", 1);
        tracker.recordFailure(id2, "Error 2", 2);
        
        assertEquals(2, tracker.getFailureCount());
        
        tracker.clear();
        
        assertEquals(0, tracker.getFailureCount());
        assertFalse(tracker.isTracked(id1));
        assertFalse(tracker.isTracked(id2));
    }

    @Test
    @DisplayName("Should respect max tracked IDs limit")
    void testMaxTrackedIdsLimit() {
        FailedIdTracker limitedTracker = new FailedIdTracker(3);
        
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();
        UUID id4 = UUID.randomUUID();
        
        limitedTracker.recordFailure(id1, "Error 1", 1);
        limitedTracker.recordFailure(id2, "Error 2", 2);
        limitedTracker.recordFailure(id3, "Error 3", 3);
        
        assertEquals(3, limitedTracker.getFailureCount());
        
        // Adding 4th should remove oldest
        limitedTracker.recordFailure(id4, "Error 4", 0);
        
        assertEquals(3, limitedTracker.getFailureCount());
        assertTrue(limitedTracker.isTracked(id4));
    }

    @Test
    @DisplayName("Should handle updating failure info for existing ID")
    void testUpdateFailureInfo() {
        UUID id = UUID.randomUUID();
        
        tracker.recordFailure(id, "Error 1", 2);
        FailedIdTracker.FailureInfo info1 = tracker.getFailureInfo(id);
        
        tracker.recordFailure(id, "Error 2", 1);
        FailedIdTracker.FailureInfo info2 = tracker.getFailureInfo(id);
        
        assertEquals("Error 2", info2.reason);
        assertEquals(1, info2.remainingRetries);
    }

    @Test
    @DisplayName("Should store failure time correctly")
    void testFailureTime() {
        UUID id = UUID.randomUUID();
        Instant beforeRecord = Instant.now();
        
        tracker.recordFailure(id, "Test error", 1);
        
        Instant afterRecord = Instant.now();
        FailedIdTracker.FailureInfo info = tracker.getFailureInfo(id);
        
        assertNotNull(info.failureTime);
        assertTrue(!info.failureTime.isBefore(beforeRecord));
        assertTrue(!info.failureTime.isAfter(afterRecord.plusSeconds(1)));
    }

    @Test
    @DisplayName("Should handle high volume of failures")
    void testHighVolumeFailures() {
        int count = 100;
        
        for (int i = 0; i < count; i++) {
            tracker.recordFailure(UUID.randomUUID(), "Error " + i, count - i);
        }
        
        assertEquals(100, tracker.getFailureCount());
        assertEquals(100, tracker.getFailedIds().size());
    }

    @Test
    @DisplayName("Should maintain thread safety with ConcurrentHashMap")
    void testThreadSafety() {
        UUID id = UUID.randomUUID();
        
        tracker.recordFailure(id, "Error", 1);
        
        // Should not throw
        assertTrue(tracker.isTracked(id));
        assertNotNull(tracker.getFailureInfo(id));
    }

    @Test
    @DisplayName("Should return immutable copy of failed IDs set")
    void testImmutableCopyOfFailedIds() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        
        tracker.recordFailure(id1, "Error 1", 1);
        
        Set<UUID> failedIds = tracker.getFailedIds();
        
        // Modifying returned set should not affect tracker
        failedIds.add(id2);
        
        // Tracker should still only have id1
        assertEquals(1, tracker.getFailureCount());
        assertFalse(tracker.isTracked(id2));
    }

    @Test
    @DisplayName("Should return immutable copy of all failures map")
    void testImmutableCopyOfAllFailures() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        
        tracker.recordFailure(id1, "Error 1", 1);
        
        Map<UUID, FailedIdTracker.FailureInfo> allFailures = tracker.getAllFailures();
        
        // Modifying returned map should not affect tracker
        allFailures.put(id2, new FailedIdTracker.FailureInfo("Error 2", 1, Instant.now()));
        
        // Tracker should still only have id1
        assertEquals(1, tracker.getFailureCount());
        assertFalse(tracker.isTracked(id2));
    }

    @Test
    @DisplayName("FailureInfo should store correct remaining retries")
    void testFailureInfoRemainingRetries() {
        UUID id = UUID.randomUUID();
        
        tracker.recordFailure(id, "Error", 3);
        assertEquals(3, tracker.getFailureInfo(id).remainingRetries);
        
        tracker.recordFailure(id, "Error", 0);
        assertEquals(0, tracker.getFailureInfo(id).remainingRetries);
        
        tracker.recordFailure(id, "Error", 5);
        assertEquals(5, tracker.getFailureInfo(id).remainingRetries);
    }
}
