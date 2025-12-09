package com.dehold.contentmanager.content.customersupport.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RetryContext Tests")
class RetryContextTest {

    private RetryContext retryContext;
    private static final int MAX_ATTEMPTS = 4;
    private static final long INITIAL_DELAY = 500;
    private static final double MULTIPLIER = 2.0;

    @BeforeEach
    void setUp() {
        retryContext = new RetryContext(MAX_ATTEMPTS, INITIAL_DELAY, MULTIPLIER);
    }

    @Test
    @DisplayName("Should initialize with correct values")
    void testInitialization() {
        assertEquals(0, retryContext.getAttemptCount());
        assertEquals(MAX_ATTEMPTS, retryContext.getMaxAttempts());
        assertTrue(retryContext.canRetry());
        assertNull(retryContext.getLastException());
        assertNull(retryContext.getTargetId());
    }

    @Test
    @DisplayName("Should initialize with target ID when provided")
    void testInitializationWithTargetId() {
        UUID targetId = UUID.randomUUID();
        RetryContext context = new RetryContext(MAX_ATTEMPTS, INITIAL_DELAY, MULTIPLIER, targetId);
        
        assertEquals(targetId, context.getTargetId());
    }

    @Test
    @DisplayName("Should record attempt and increment count")
    void testRecordAttempt() {
        Exception exception = new RuntimeException("Test error");
        
        retryContext.recordAttempt(exception);
        
        assertEquals(1, retryContext.getAttemptCount());
        assertEquals(exception, retryContext.getLastException());
        assertNotNull(retryContext.getLastAttemptTime());
    }

    @Test
    @DisplayName("Should return true for canRetry until max attempts reached")
    void testCanRetry() {
        assertTrue(retryContext.canRetry()); // 0 < 4
        
        retryContext.recordAttempt(new RuntimeException());
        assertTrue(retryContext.canRetry()); // 1 < 4
        
        retryContext.recordAttempt(new RuntimeException());
        assertTrue(retryContext.canRetry()); // 2 < 4
        
        retryContext.recordAttempt(new RuntimeException());
        assertTrue(retryContext.canRetry()); // 3 < 4
        
        retryContext.recordAttempt(new RuntimeException());
        assertFalse(retryContext.canRetry()); // 4 >= 4
    }

    @Test
    @DisplayName("Should calculate exponential backoff correctly")
    void testExponentialBackoffCalculation() {
        // First retry: 500 * 2^0 = 500
        retryContext.recordAttempt(new RuntimeException());
        assertEquals(500, retryContext.getNextBackoffDelayMs());
        
        // Second retry: 500 * 2^1 = 1000
        retryContext.recordAttempt(new RuntimeException());
        assertEquals(1000, retryContext.getNextBackoffDelayMs());
        
        // Third retry: 500 * 2^2 = 2000
        retryContext.recordAttempt(new RuntimeException());
        assertEquals(2000, retryContext.getNextBackoffDelayMs());
        
        // Fourth retry: 500 * 2^3 = 4000
        retryContext.recordAttempt(new RuntimeException());
        assertEquals(4000, retryContext.getNextBackoffDelayMs());
    }

    @Test
    @DisplayName("Should track last attempt time")
    void testLastAttemptTime() {
        Instant beforeAttempt = Instant.now();
        
        retryContext.recordAttempt(new RuntimeException());
        
        Instant afterAttempt = Instant.now();
        Instant lastTime = retryContext.getLastAttemptTime();
        
        assertNotNull(lastTime);
        assertTrue(!lastTime.isBefore(beforeAttempt));
        assertTrue(!lastTime.isAfter(afterAttempt.plusSeconds(1)));
    }

    @Test
    @DisplayName("Should calculate time since last attempt")
    void testTimeSinceLastAttempt() throws InterruptedException {
        retryContext.recordAttempt(new RuntimeException());
        
        Thread.sleep(100);
        
        Duration duration = retryContext.getTimeSinceLastAttempt();
        
        assertNotNull(duration);
        assertTrue(duration.toMillis() >= 100);
    }

    @Test
    @DisplayName("Should store and retrieve last exception")
    void testLastExceptionStorage() {
        RuntimeException exception = new RuntimeException("Test error message");
        
        assertNull(retryContext.getLastException());
        
        retryContext.recordAttempt(exception);
        
        assertEquals(exception, retryContext.getLastException());
        assertEquals("Test error message", retryContext.getLastException().getMessage());
    }

    @Test
    @DisplayName("Should handle multiple different exceptions")
    void testMultipleExceptions() {
        RuntimeException exception1 = new RuntimeException("Error 1");
        IOException exception2 = new IOException("Error 2");
        
        retryContext.recordAttempt(exception1);
        assertEquals(exception1, retryContext.getLastException());
        
        retryContext.recordAttempt(exception2);
        assertEquals(exception2, retryContext.getLastException());
    }

    @Test
    @DisplayName("Should handle different backoff multipliers")
    void testDifferentMultipliers() {
        RetryContext context1 = new RetryContext(4, 100, 1.5);
        RetryContext context2 = new RetryContext(4, 100, 3.0);
        
        context1.recordAttempt(new RuntimeException());
        context2.recordAttempt(new RuntimeException());
        
        // First retry: 100 * 1.5^0 = 100, 100 * 3.0^0 = 100
        assertEquals(100, context1.getNextBackoffDelayMs());
        assertEquals(100, context2.getNextBackoffDelayMs());
    }

    @Test
    @DisplayName("Should handle edge case with multiplier of 1")
    void testMultiplierOfOne() {
        RetryContext context = new RetryContext(4, 100, 1.0);
        
        context.recordAttempt(new RuntimeException());
        assertEquals(100, context.getNextBackoffDelayMs());
        
        context.recordAttempt(new RuntimeException());
        assertEquals(100, context.getNextBackoffDelayMs());
        
        context.recordAttempt(new RuntimeException());
        assertEquals(100, context.getNextBackoffDelayMs());
    }

    @Test
    @DisplayName("Should validate state consistency after multiple operations")
    void testStateConsistency() {
        UUID targetId = UUID.randomUUID();
        RetryContext context = new RetryContext(3, 100, 2.0, targetId);
        
        // Verify initial state
        assertEquals(0, context.getAttemptCount());
        assertEquals(targetId, context.getTargetId());
        assertTrue(context.canRetry());
        
        // Record attempts
        for (int i = 0; i < 3; i++) {
            Exception ex = new RuntimeException("Error " + (i + 1));
            context.recordAttempt(ex);
            assertEquals(i + 1, context.getAttemptCount());
            assertEquals(ex, context.getLastException());
            assertEquals(i < 2, context.canRetry());
        }
    }
}
