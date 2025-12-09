package com.dehold.contentmanager.content.customersupport.service;

import com.dehold.contentmanager.content.customersupport.model.SupportRequest;
import com.dehold.contentmanager.content.customersupport.repository.SupportRequestRepository;
import com.dehold.contentmanager.content.customersupport.util.FailedIdTracker;
import com.dehold.contentmanager.content.customersupport.web.dto.PartialResultResponse;
import com.dehold.contentmanager.exception.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("SupportRequestService Retry Logic Tests")
class SupportRequestServiceRetryLogicTest {

    @Mock
    private SupportRequestRepository repository;

    private SupportRequestService service;
    private FailedIdTracker failedIdTracker;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        failedIdTracker = new FailedIdTracker();
        service = new SupportRequestService(repository, failedIdTracker);
    }

    // ==================== findAllWithRetry Tests ====================

    @Test
    @DisplayName("findAllWithRetry should succeed on first attempt with no failures")
    void testFindAllWithRetry_SuccessOnFirstAttempt() {
        // Arrange
        List<SupportRequest> expectedRequests = createTestRequests(3);
        when(repository.findAll()).thenReturn(expectedRequests);

        // Act
        PartialResultResponse<SupportRequest> response = service.findAllWithRetry();

        // Assert
        assertNotNull(response);
        assertEquals(3, response.getSuccessCount());
        assertEquals(0, response.getFailureCount());
        assertFalse(response.isPartial());
        assertEquals(expectedRequests.size(), response.getSuccessfulResults().size());
        assertTrue(failedIdTracker.getFailedIds().isEmpty());
        verify(repository, times(1)).findAll();
    }

    @Test
    @DisplayName("findAllWithRetry should retry on transient failure and succeed")
    void testFindAllWithRetry_SuccessAfterOneRetry() {
        // Arrange
        List<SupportRequest> expectedRequests = createTestRequests(2);
        when(repository.findAll())
                .thenThrow(new TransientDataAccessResourceException("DB network issue"))
                .thenReturn(expectedRequests);

        // Act
        PartialResultResponse<SupportRequest> response = service.findAllWithRetry();

        // Assert
        assertNotNull(response);
        assertEquals(2, response.getSuccessCount());
        assertEquals(0, response.getFailureCount());
        assertFalse(response.isPartial());
        verify(repository, times(2)).findAll();
    }

    @Test
    @DisplayName("findAllWithRetry should retry multiple times and eventually succeed")
    void testFindAllWithRetry_SuccessAfterMultipleRetries() {
        // Arrange
        List<SupportRequest> expectedRequests = createTestRequests(1);
        when(repository.findAll())
                .thenThrow(new TransientDataAccessResourceException("DB issue 1"))
                .thenThrow(new TransientDataAccessResourceException("DB issue 2"))
                .thenThrow(new TransientDataAccessResourceException("DB issue 3"))
                .thenReturn(expectedRequests);

        // Act
        PartialResultResponse<SupportRequest> response = service.findAllWithRetry();

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getSuccessCount());
        verify(repository, times(4)).findAll();
    }

    @Test
    @DisplayName("findAllWithRetry should throw after max retries exhausted")
    void testFindAllWithRetry_FailureAfterMaxRetries() {
        // Arrange
        when(repository.findAll())
                .thenThrow(new TransientDataAccessResourceException("DB issue 1"))
                .thenThrow(new TransientDataAccessResourceException("DB issue 2"))
                .thenThrow(new TransientDataAccessResourceException("DB issue 3"))
                .thenThrow(new TransientDataAccessResourceException("DB issue 4"));

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            service.findAllWithRetry();
        });

        assertEquals(503, exception.getStatusCode().value()); // SERVICE_UNAVAILABLE
        assertTrue(exception.getReason().contains("temporarily unavailable"));
        verify(repository, times(4)).findAll();
    }

    @Test
    @DisplayName("findAllWithRetry should return retry info with correct attempt count")
    void testFindAllWithRetry_RetryInfoAccuracy() {
        // Arrange
        List<SupportRequest> expectedRequests = createTestRequests(1);
        when(repository.findAll())
                .thenThrow(new TransientDataAccessResourceException("Error"))
                .thenReturn(expectedRequests);

        // Act
        PartialResultResponse<SupportRequest> response = service.findAllWithRetry();

        // Assert
        assertNotNull(response.getRetryInfo());
        assertEquals(1, response.getRetryInfo().getCurrentAttempt()); // Only 1 attempt needed (success on 2nd)
        assertEquals(4, response.getRetryInfo().getMaxRetries());
        assertEquals("Exponential backoff with multiplier 2.0", response.getRetryInfo().getRetryStrategy());
    }

    @Test
    @DisplayName("findAllWithRetry should return empty list when no requests found")
    void testFindAllWithRetry_EmptyResultSet() {
        // Arrange
        when(repository.findAll()).thenReturn(new ArrayList<>());

        // Act
        PartialResultResponse<SupportRequest> response = service.findAllWithRetry();

        // Assert
        assertNotNull(response);
        assertEquals(0, response.getSuccessCount());
        assertTrue(response.getSuccessfulResults().isEmpty());
        assertFalse(response.isPartial());
    }

    // ==================== findByIdWithRetry Tests ====================

    @Test
    @DisplayName("findByIdWithRetry should succeed on first attempt when entity found")
    void testFindByIdWithRetry_SuccessOnFirstAttempt() {
        // Arrange
        UUID id = UUID.randomUUID();
        SupportRequest request = createTestRequest(id);
        when(repository.getById(id)).thenReturn(Optional.of(request));

        // Act
        PartialResultResponse<SupportRequest> response = service.findByIdWithRetry(id);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getSuccessCount());
        assertEquals(0, response.getFailureCount());
        assertFalse(response.isPartial());
        assertEquals(request.getId(), response.getSuccessfulResults().get(0).getId());
        assertFalse(failedIdTracker.isTracked(id));
        verify(repository, times(1)).getById(id);
    }

    @Test
    @DisplayName("findByIdWithRetry should retry on transient failure and succeed")
    void testFindByIdWithRetry_SuccessAfterOneRetry() {
        // Arrange
        UUID id = UUID.randomUUID();
        SupportRequest request = createTestRequest(id);
        when(repository.getById(id))
                .thenThrow(new TransientDataAccessResourceException("Network timeout"))
                .thenReturn(Optional.of(request));

        // Act
        PartialResultResponse<SupportRequest> response = service.findByIdWithRetry(id);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getSuccessCount());
        assertEquals(0, response.getFailureCount());
        assertFalse(response.isPartial());
        verify(repository, times(2)).getById(id);
    }

    @Test
    @DisplayName("findByIdWithRetry should track failed ID after max retries")
    void testFindByIdWithRetry_TrackFailedIdAfterMaxRetries() {
        // Arrange
        UUID id = UUID.randomUUID();
        when(repository.getById(id))
                .thenThrow(new TransientDataAccessResourceException("Error 1"))
                .thenThrow(new TransientDataAccessResourceException("Error 2"))
                .thenThrow(new TransientDataAccessResourceException("Error 3"))
                .thenThrow(new TransientDataAccessResourceException("Error 4"));

        // Act
        PartialResultResponse<SupportRequest> response = service.findByIdWithRetry(id);

        // Assert
        assertNotNull(response);
        assertEquals(0, response.getSuccessCount());
        assertEquals(1, response.getFailureCount());
        assertTrue(response.isPartial());
        assertTrue(response.getFailedIds().containsKey(id));
        assertEquals(0, response.getFailedIds().get(id).getRemainingRetries());
        verify(repository, times(4)).getById(id);
    }

    @Test
    @DisplayName("findByIdWithRetry should throw when entity not found (not retryable)")
    void testFindByIdWithRetry_EntityNotFound() {
        // Arrange
        UUID id = UUID.randomUUID();
        when(repository.getById(id)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> {
            service.findByIdWithRetry(id);
        });

        verify(repository, times(1)).getById(id);
    }

    @Test
    @DisplayName("findByIdWithRetry should update retry info with correct values")
    void testFindByIdWithRetry_RetryInfoAccuracy() {
        // Arrange
        UUID id = UUID.randomUUID();
        SupportRequest request = createTestRequest(id);
        when(repository.getById(id))
                .thenThrow(new TransientDataAccessResourceException("Error"))
                .thenReturn(Optional.of(request));

        // Act
        PartialResultResponse<SupportRequest> response = service.findByIdWithRetry(id);

        // Assert
        assertNotNull(response.getRetryInfo());
        assertEquals(1, response.getRetryInfo().getCurrentAttempt()); // Only 1 attempt needed (success on 2nd)
        assertEquals(4, response.getRetryInfo().getMaxRetries());
        assertTrue(response.getRetryInfo().getRetryAfterMs() > 0);
    }

    @Test
    @DisplayName("findByIdWithRetry should maintain failed ID tracker state")
    void testFindByIdWithRetry_FailedIdTrackerState() {
        // Arrange
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        
        when(repository.getById(id1))
                .thenThrow(new TransientDataAccessResourceException("Error 1"))
                .thenThrow(new TransientDataAccessResourceException("Error 2"))
                .thenThrow(new TransientDataAccessResourceException("Error 3"))
                .thenThrow(new TransientDataAccessResourceException("Error 4"));
        
        when(repository.getById(id2)).thenReturn(Optional.of(createTestRequest(id2)));

        // Act
        PartialResultResponse<SupportRequest> response1 = service.findByIdWithRetry(id1);
        PartialResultResponse<SupportRequest> response2 = service.findByIdWithRetry(id2);

        // Assert
        assertTrue(failedIdTracker.isTracked(id1));
        assertFalse(failedIdTracker.isTracked(id2));
        
        FailedIdTracker.FailureInfo failureInfo = failedIdTracker.getFailureInfo(id1);
        assertNotNull(failureInfo);
        assertEquals(0, failureInfo.remainingRetries);
    }

    // ==================== findByIdsWithRetry Tests ====================

    @Test
    @DisplayName("findByIdsWithRetry should return all successful when all succeed")
    void testFindByIdsWithRetry_AllSuccessful() {
        // Arrange
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();
        
        SupportRequest req1 = createTestRequest(id1);
        SupportRequest req2 = createTestRequest(id2);
        SupportRequest req3 = createTestRequest(id3);
        
        when(repository.getById(id1)).thenReturn(Optional.of(req1));
        when(repository.getById(id2)).thenReturn(Optional.of(req2));
        when(repository.getById(id3)).thenReturn(Optional.of(req3));

        // Act
        PartialResultResponse<SupportRequest> response = service.findByIdsWithRetry(
                List.of(id1, id2, id3)
        );

        // Assert
        assertNotNull(response);
        assertEquals(3, response.getSuccessCount());
        assertEquals(0, response.getFailureCount());
        assertFalse(response.isPartial());
    }

    @Test
    @DisplayName("findByIdsWithRetry should return partial results with mixed success and failures")
    void testFindByIdsWithRetry_PartialResults() {
        // Arrange
        UUID successId = UUID.randomUUID();
        UUID failedId = UUID.randomUUID();
        UUID notFoundId = UUID.randomUUID();
        
        when(repository.getById(successId))
                .thenReturn(Optional.of(createTestRequest(successId)));
        
        when(repository.getById(failedId))
                .thenThrow(new TransientDataAccessResourceException("Error 1"))
                .thenThrow(new TransientDataAccessResourceException("Error 2"))
                .thenThrow(new TransientDataAccessResourceException("Error 3"))
                .thenThrow(new TransientDataAccessResourceException("Error 4"));
        
        when(repository.getById(notFoundId))
                .thenReturn(Optional.empty());

        // Act
        PartialResultResponse<SupportRequest> response = service.findByIdsWithRetry(
                List.of(successId, failedId, notFoundId)
        );

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getSuccessCount());
        assertEquals(2, response.getFailureCount());
        assertTrue(response.isPartial());
        assertTrue(response.getFailedIds().containsKey(failedId));
        assertTrue(response.getFailedIds().containsKey(notFoundId));
    }

    @Test
    @DisplayName("findByIdsWithRetry should handle empty input list")
    void testFindByIdsWithRetry_EmptyInputList() {
        // Act
        PartialResultResponse<SupportRequest> response = service.findByIdsWithRetry(
                new ArrayList<>()
        );

        // Assert
        assertNotNull(response);
        assertEquals(0, response.getSuccessCount());
        assertEquals(0, response.getFailureCount());
        assertFalse(response.isPartial());
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("findByIdsWithRetry should handle all failures")
    void testFindByIdsWithRetry_AllFailures() {
        // Arrange
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        
        when(repository.getById(id1))
                .thenThrow(new TransientDataAccessResourceException("Error 1"))
                .thenThrow(new TransientDataAccessResourceException("Error 2"))
                .thenThrow(new TransientDataAccessResourceException("Error 3"))
                .thenThrow(new TransientDataAccessResourceException("Error 4"));
        
        when(repository.getById(id2))
                .thenReturn(Optional.empty());

        // Act
        PartialResultResponse<SupportRequest> response = service.findByIdsWithRetry(
                List.of(id1, id2)
        );

        // Assert
        assertNotNull(response);
        assertEquals(0, response.getSuccessCount());
        assertEquals(2, response.getFailureCount());
        assertTrue(response.isPartial());
    }

    // ==================== FailedIdTracker Tests ====================

    @Test
    @DisplayName("getFailedIdTracker should return valid tracker instance")
    void testGetFailedIdTracker() {
        // Act
        FailedIdTracker tracker = service.getFailedIdTracker();

        // Assert
        assertNotNull(tracker);
        assertSame(failedIdTracker, tracker);
    }

    @Test
    @DisplayName("clearFailedIdTracker should clear all tracked failures")
    void testClearFailedIdTracker() {
        // Arrange
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        failedIdTracker.recordFailure(id1, "Test error 1", 2);
        failedIdTracker.recordFailure(id2, "Test error 2", 1);

        assertTrue(failedIdTracker.isTracked(id1));
        assertTrue(failedIdTracker.isTracked(id2));

        // Act
        service.clearFailedIdTracker();

        // Assert
        assertFalse(failedIdTracker.isTracked(id1));
        assertFalse(failedIdTracker.isTracked(id2));
        assertTrue(failedIdTracker.getFailedIds().isEmpty());
    }

    // ==================== Edge Cases and Boundary Tests ====================

    @Test
    @DisplayName("findByIdWithRetry should handle concurrent failures and retries correctly")
    void testFindByIdWithRetry_ConcurrentRetries() {
        // Arrange
        UUID id = UUID.randomUUID();
        SupportRequest request = createTestRequest(id);
        
        // Simulate varying retry scenarios
        when(repository.getById(id))
                .thenThrow(new TransientDataAccessResourceException("Timeout"))
                .thenThrow(new TransientDataAccessResourceException("Connection reset"))
                .thenReturn(Optional.of(request));

        // Act
        PartialResultResponse<SupportRequest> response = service.findByIdWithRetry(id);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getSuccessCount());
        verify(repository, times(3)).getById(id);
    }

    @Test
    @DisplayName("findByIdWithRetry should correctly calculate remaining retries in failed ID info")
    void testFindByIdWithRetry_RemainingRetriesCalculation() {
        // Arrange
        UUID id = UUID.randomUUID();
        when(repository.getById(id))
                .thenThrow(new TransientDataAccessResourceException("Error 1"))
                .thenThrow(new TransientDataAccessResourceException("Error 2"))
                .thenThrow(new TransientDataAccessResourceException("Error 3"))
                .thenThrow(new TransientDataAccessResourceException("Error 4"));

        // Act
        PartialResultResponse<SupportRequest> response = service.findByIdWithRetry(id);

        // Assert
        PartialResultResponse.FailedIdInfo failedInfo = response.getFailedIds().get(id);
        assertNotNull(failedInfo);
        assertEquals(0, failedInfo.getRemainingRetries()); // 4 - 4 = 0
        assertTrue(failedInfo.getFailureReason().contains("retry attempts"));
    }

    @Test
    @DisplayName("findAllWithRetry should clear previous failures on success")
    void testFindAllWithRetry_ClearsPreviousFailures() {
        // Arrange
        UUID id = UUID.randomUUID();
        failedIdTracker.recordFailure(id, "Previous error", 2);
        
        assertTrue(failedIdTracker.isTracked(id));

        List<SupportRequest> requests = createTestRequests(1);
        when(repository.findAll()).thenReturn(requests);

        // Act
        PartialResultResponse<SupportRequest> response = service.findAllWithRetry();

        // Assert
        assertNotNull(response);
        assertFalse(failedIdTracker.isTracked(id));
    }

    @Test
    @DisplayName("Response timestamp should be set correctly")
    void testFindByIdWithRetry_ResponseTimestamp() {
        // Arrange
        UUID id = UUID.randomUUID();
        Instant beforeCall = Instant.now();
        
        when(repository.getById(id)).thenReturn(Optional.of(createTestRequest(id)));

        // Act
        PartialResultResponse<SupportRequest> response = service.findByIdWithRetry(id);

        Instant afterCall = Instant.now();

        // Assert
        assertNotNull(response.getTimestamp());
        assertFalse(response.getTimestamp().isBefore(beforeCall));
        assertFalse(response.getTimestamp().isAfter(afterCall.plusSeconds(1)));
    }

    // ==================== Helper Methods ====================

    private SupportRequest createTestRequest(UUID id) {
        return new SupportRequest(
                id,
                UUID.randomUUID(),
                "Test request",
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.now(),
                Instant.now()
        );
    }

    private List<SupportRequest> createTestRequests(int count) {
        List<SupportRequest> requests = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            requests.add(createTestRequest(UUID.randomUUID()));
        }
        return requests;
    }
}
