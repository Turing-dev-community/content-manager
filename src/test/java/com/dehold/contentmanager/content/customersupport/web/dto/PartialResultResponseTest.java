package com.dehold.contentmanager.content.customersupport.web.dto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PartialResultResponse Tests")
class PartialResultResponseTest {

    private PartialResultResponse<String> response;

    @BeforeEach
    void setUp() {
        response = new PartialResultResponse<>();
    }

    @Test
    @DisplayName("Should initialize with default values")
    void testInitialization() {
        assertFalse(response.isPartial());
        assertNotNull(response.getTimestamp());
        assertEquals(0, response.getSuccessCount());
        assertEquals(0, response.getFailureCount());
    }

    @Test
    @DisplayName("Should initialize with successful results")
    void testInitializationWithResults() {
        List<String> results = List.of("item1", "item2");
        response = new PartialResultResponse<>(results);
        
        assertEquals(2, response.getSuccessCount());
        assertEquals(results, response.getSuccessfulResults());
        assertFalse(response.isPartial());
    }

    @Test
    @DisplayName("Should initialize with results and failed IDs")
    void testInitializationWithResultsAndFailures() {
        List<String> results = List.of("item1");
        Map<UUID, PartialResultResponse.FailedIdInfo> failedIds = new HashMap<>();
        UUID failedId = UUID.randomUUID();
        failedIds.put(failedId, new PartialResultResponse.FailedIdInfo(
                failedId, "Not found", 0
        ));
        
        response = new PartialResultResponse<>(results, failedIds);
        
        assertEquals(1, response.getSuccessCount());
        assertEquals(1, response.getFailureCount());
        assertTrue(response.isPartial());
    }

    @Test
    @DisplayName("Should set and get successful results")
    void testSuccessfulResults() {
        List<String> results = List.of("a", "b", "c");
        response.setSuccessfulResults(results);
        
        assertEquals(3, response.getSuccessCount());
        assertEquals(results, response.getSuccessfulResults());
    }

    @Test
    @DisplayName("Should set and get failed IDs")
    void testFailedIds() {
        Map<UUID, PartialResultResponse.FailedIdInfo> failedIds = new HashMap<>();
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        
        failedIds.put(id1, new PartialResultResponse.FailedIdInfo(id1, "Error 1", 2));
        failedIds.put(id2, new PartialResultResponse.FailedIdInfo(id2, "Error 2", 1));
        
        response.setFailedIds(failedIds);
        
        assertEquals(2, response.getFailureCount());
        assertTrue(response.getFailedIds().containsKey(id1));
        assertTrue(response.getFailedIds().containsKey(id2));
    }

    @Test
    @DisplayName("Should set and get retry info")
    void testRetryInfo() {
        PartialResultResponse.RetryInfo retryInfo = 
                new PartialResultResponse.RetryInfo(1000, "Exponential backoff", 4, 2);
        
        response.setRetryInfo(retryInfo);
        
        assertNotNull(response.getRetryInfo());
        assertEquals(1000, response.getRetryInfo().getRetryAfterMs());
        assertEquals("Exponential backoff", response.getRetryInfo().getRetryStrategy());
        assertEquals(4, response.getRetryInfo().getMaxRetries());
        assertEquals(2, response.getRetryInfo().getCurrentAttempt());
    }

    @Test
    @DisplayName("Should set and get partial flag")
    void testPartialFlag() {
        assertFalse(response.isPartial());
        
        response.setPartial(true);
        assertTrue(response.isPartial());
        
        response.setPartial(false);
        assertFalse(response.isPartial());
    }

    @Test
    @DisplayName("Should set and get timestamp")
    void testTimestamp() {
        Instant now = Instant.now();
        response.setTimestamp(now);
        
        assertEquals(now, response.getTimestamp());
    }

    @Test
    @DisplayName("Should calculate success count correctly")
    void testSuccessCount() {
        assertEquals(0, response.getSuccessCount());
        
        response.setSuccessfulResults(List.of("a"));
        assertEquals(1, response.getSuccessCount());
        
        response.setSuccessfulResults(List.of("a", "b", "c"));
        assertEquals(3, response.getSuccessCount());
        
        response.setSuccessfulResults(new ArrayList<>());
        assertEquals(0, response.getSuccessCount());
    }

    @Test
    @DisplayName("Should calculate failure count correctly")
    void testFailureCount() {
        assertEquals(0, response.getFailureCount());
        
        Map<UUID, PartialResultResponse.FailedIdInfo> failedIds = new HashMap<>();
        failedIds.put(UUID.randomUUID(), new PartialResultResponse.FailedIdInfo(
                UUID.randomUUID(), "Error", 1
        ));
        response.setFailedIds(failedIds);
        
        assertEquals(1, response.getFailureCount());
        
        failedIds.put(UUID.randomUUID(), new PartialResultResponse.FailedIdInfo(
                UUID.randomUUID(), "Error", 0
        ));
        response.setFailedIds(failedIds);
        
        assertEquals(2, response.getFailureCount());
    }

    @Test
    @DisplayName("Should handle null successful results in success count")
    void testSuccessCountWithNullResults() {
        response.setSuccessfulResults(null);
        assertEquals(0, response.getSuccessCount());
    }

    @Test
    @DisplayName("Should handle null failed IDs in failure count")
    void testFailureCountWithNullFailedIds() {
        response.setFailedIds(null);
        assertEquals(0, response.getFailureCount());
    }

    // ==================== FailedIdInfo Tests ====================

    @Test
    @DisplayName("FailedIdInfo should initialize with default values")
    void testFailedIdInfoInitialization() {
        PartialResultResponse.FailedIdInfo info = new PartialResultResponse.FailedIdInfo();
        
        assertNull(info.getId());
        assertNull(info.getFailureReason());
        assertEquals(0, info.getRemainingRetries());
        assertNull(info.getFailureTime());
    }

    @Test
    @DisplayName("FailedIdInfo should initialize with provided values")
    void testFailedIdInfoInitializationWithValues() {
        UUID id = UUID.randomUUID();
        String reason = "Database timeout";
        int retries = 2;
        
        PartialResultResponse.FailedIdInfo info = 
                new PartialResultResponse.FailedIdInfo(id, reason, retries);
        
        assertEquals(id, info.getId());
        assertEquals(reason, info.getFailureReason());
        assertEquals(retries, info.getRemainingRetries());
        assertNotNull(info.getFailureTime());
    }

    @Test
    @DisplayName("FailedIdInfo should set and get all properties")
    void testFailedIdInfoProperties() {
        PartialResultResponse.FailedIdInfo info = new PartialResultResponse.FailedIdInfo();
        UUID id = UUID.randomUUID();
        Instant time = Instant.now();
        
        info.setId(id);
        info.setFailureReason("Network error");
        info.setRemainingRetries(3);
        info.setFailureTime(time);
        
        assertEquals(id, info.getId());
        assertEquals("Network error", info.getFailureReason());
        assertEquals(3, info.getRemainingRetries());
        assertEquals(time, info.getFailureTime());
    }

    // ==================== RetryInfo Tests ====================

    @Test
    @DisplayName("RetryInfo should initialize with default values")
    void testRetryInfoInitialization() {
        PartialResultResponse.RetryInfo info = new PartialResultResponse.RetryInfo();
        
        assertEquals(0, info.getRetryAfterMs());
        assertNull(info.getRetryStrategy());
        assertEquals(0, info.getMaxRetries());
        assertEquals(0, info.getCurrentAttempt());
    }

    @Test
    @DisplayName("RetryInfo should initialize with provided values")
    void testRetryInfoInitializationWithValues() {
        PartialResultResponse.RetryInfo info = 
                new PartialResultResponse.RetryInfo(1500, "Linear backoff", 5, 2);
        
        assertEquals(1500, info.getRetryAfterMs());
        assertEquals("Linear backoff", info.getRetryStrategy());
        assertEquals(5, info.getMaxRetries());
        assertEquals(2, info.getCurrentAttempt());
    }

    @Test
    @DisplayName("RetryInfo should set and get all properties")
    void testRetryInfoProperties() {
        PartialResultResponse.RetryInfo info = new PartialResultResponse.RetryInfo();
        
        info.setRetryAfterMs(2000);
        info.setRetryStrategy("Exponential backoff");
        info.setMaxRetries(4);
        info.setCurrentAttempt(3);
        
        assertEquals(2000, info.getRetryAfterMs());
        assertEquals("Exponential backoff", info.getRetryStrategy());
        assertEquals(4, info.getMaxRetries());
        assertEquals(3, info.getCurrentAttempt());
    }

    @Test
    @DisplayName("RetryInfo should handle large retry values")
    void testRetryInfoLargeValues() {
        PartialResultResponse.RetryInfo info = new PartialResultResponse.RetryInfo(
                Long.MAX_VALUE, "Custom strategy", Integer.MAX_VALUE, Integer.MAX_VALUE - 1
        );
        
        assertEquals(Long.MAX_VALUE, info.getRetryAfterMs());
        assertEquals(Integer.MAX_VALUE, info.getMaxRetries());
        assertEquals(Integer.MAX_VALUE - 1, info.getCurrentAttempt());
    }

    // ==================== Integration Tests ====================

    @Test
    @DisplayName("Should build complete response with all data")
    void testCompleteResponse() {
        // Setup successful results
        List<String> results = List.of("result1", "result2");
        response.setSuccessfulResults(results);
        
        // Setup failed IDs
        Map<UUID, PartialResultResponse.FailedIdInfo> failedIds = new HashMap<>();
        UUID failedId1 = UUID.randomUUID();
        UUID failedId2 = UUID.randomUUID();
        failedIds.put(failedId1, new PartialResultResponse.FailedIdInfo(
                failedId1, "Timeout", 2
        ));
        failedIds.put(failedId2, new PartialResultResponse.FailedIdInfo(
                failedId2, "Connection reset", 1
        ));
        response.setFailedIds(failedIds);
        
        // Setup retry info
        PartialResultResponse.RetryInfo retryInfo = 
                new PartialResultResponse.RetryInfo(1000, "Exponential backoff", 4, 2);
        response.setRetryInfo(retryInfo);
        
        response.setPartial(true);
        Instant timestamp = Instant.now();
        response.setTimestamp(timestamp);
        
        // Verify
        assertEquals(2, response.getSuccessCount());
        assertEquals(2, response.getFailureCount());
        assertTrue(response.isPartial());
        assertEquals(timestamp, response.getTimestamp());
        assertNotNull(response.getRetryInfo());
        assertEquals(2, response.getRetryInfo().getCurrentAttempt());
    }

    @Test
    @DisplayName("Should handle complex generic type")
    void testComplexGenericType() {
        PartialResultResponse<Map<String, Object>> complexResponse = 
                new PartialResultResponse<>();
        
        List<Map<String, Object>> results = new ArrayList<>();
        Map<String, Object> item = new HashMap<>();
        item.put("key", "value");
        results.add(item);
        
        complexResponse.setSuccessfulResults(results);
        
        assertEquals(1, complexResponse.getSuccessCount());
        assertEquals("value", complexResponse.getSuccessfulResults().get(0).get("key"));
    }
}
