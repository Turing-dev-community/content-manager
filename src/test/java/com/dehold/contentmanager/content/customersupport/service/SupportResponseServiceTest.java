package com.dehold.contentmanager.content.customersupport.service;

import com.dehold.contentmanager.content.customersupport.model.SupportResponse;
import com.dehold.contentmanager.content.customersupport.repository.SupportResponseRepository;
import com.dehold.contentmanager.exception.EntityNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.retry.annotation.Retryable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
class SupportResponseServiceTest {
    @MockitoBean
    private SupportResponseRepository repository;

    @Autowired
    private SupportResponseServiceImpl service;

    @Autowired 
    private CacheManager cacheManager;

    @Test
    void createSupportResponse_shouldCallRepository() {
        SupportResponse response = new SupportResponse(UUID.randomUUID(), UUID.randomUUID(), "Test text",
                UUID.randomUUID(), Instant.now(), Instant.now());
        doNothing().when(repository).create(any(SupportResponse.class));
        service.createSupportResponse(response);
        verify(repository, times(1)).create(any(SupportResponse.class));
    }

    @Test
    void getSupportResponse_shouldReturnResponseIfExists() {
        UUID id = UUID.randomUUID();
        SupportResponse response = new SupportResponse(id, UUID.randomUUID(),"Test text", UUID.randomUUID(),
                Instant.now(), Instant.now());
        when(repository.getById(id)).thenReturn(Optional.of(response));
        SupportResponse found = service.getSupportResponse(id);
        assertEquals(response.getId(), found.getId());
        verify(repository, times(1)).getById(id);
    }

    @Test
    void updateSupportResponse_shouldCallRepository() {
        SupportResponse response = new SupportResponse(UUID.randomUUID(), UUID.randomUUID(), "Updated text", UUID.randomUUID(),
                Instant.now(), Instant.now());
        doNothing().when(repository).update(any(SupportResponse.class));
        service.updateSupportResponse(response);
        verify(repository, times(1)).update(any(SupportResponse.class));
    }

    @Test
    void deleteSupportResponse_shouldCallRepository() {
        UUID id = UUID.randomUUID();
        doNothing().when(repository).delete(id);
        service.deleteSupportResponse(id);
        verify(repository, times(1)).delete(id);
    }

    @Test
    void getSupportResponse_shouldCacheResultOnSecondCall() {
        clearCaches();
        UUID id = UUID.randomUUID();
        SupportResponse response = new SupportResponse(id, UUID.randomUUID(), "Test text", UUID.randomUUID(),
                Instant.now(), Instant.now());
        
        when(repository.getById(id)).thenReturn(Optional.of(response));

        SupportResponse firstFound = service.getSupportResponse(id);
        assertEquals(response.getId(), firstFound.getId());

        SupportResponse secondFound = service.getSupportResponse(id);
        assertEquals(response.getId(), secondFound.getId());

        verify(repository, times(1)).getById(id);
    }
    
    @Test
    void getSupportResponsesByUserId_shouldCacheResultOnSecondCall() {
        clearCaches();
        UUID userId = UUID.randomUUID();
        SupportResponse response = new SupportResponse(UUID.randomUUID(), userId, "Test text", UUID.randomUUID(),
                Instant.now(), Instant.now());
        List<SupportResponse> responseList = Collections.singletonList(response);
        
        when(repository.getSupportResponsesByUserId(userId)).thenReturn(responseList);

        List<SupportResponse> firstList = service.getSupportResponsesByUserId(userId);
        assertFalse(firstList.isEmpty());

        List<SupportResponse> secondList = service.getSupportResponsesByUserId(userId);
        assertFalse(secondList.isEmpty());

        verify(repository, times(1)).getSupportResponsesByUserId(userId);
    }

    @Test
    void createSupportResponse_shouldEvictResponseListCache() {
        clearCaches();

        UUID userId = UUID.randomUUID();
        SupportResponse response1 = new SupportResponse(UUID.randomUUID(), userId, "Old Response", UUID.randomUUID(), Instant.now(), Instant.now());
        SupportResponse response2 = new SupportResponse(UUID.randomUUID(), userId, "New Response", UUID.randomUUID(), Instant.now(), Instant.now());

        // 1. Mock repository to return different lists on subsequent calls
        when(repository.getSupportResponsesByUserId(userId))
            .thenReturn(Collections.singletonList(response1)) // 1st call
            .thenReturn(List.of(response1, response2)); // 2nd call after eviction

        // 2. Call to cache the initial list
        service.getSupportResponsesByUserId(userId);
        verify(repository, times(1)).getSupportResponsesByUserId(userId);

        // 3. Create a new response (should trigger eviction of the list cache)
        doNothing().when(repository).create(any(SupportResponse.class));
        service.createSupportResponse(response2);
        
        // 4. Call again (should hit the repository due to eviction)
        service.getSupportResponsesByUserId(userId);

        // Verification: Repository should be called twice
        verify(repository, times(2)).getSupportResponsesByUserId(userId);
    }

    @Test
    void updateSupportResponse_shouldEvictIdAndListCaches() {
        clearCaches();
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        SupportResponse original = new SupportResponse(id, userId, "Original", UUID.randomUUID(), Instant.now(), Instant.now());
        SupportResponse updated = new SupportResponse(id, userId, "Updated", UUID.randomUUID(), Instant.now(), Instant.now());

        // 1. Set up mocks for cache priming
        when(repository.getById(id))
            .thenReturn(Optional.of(original))
            .thenReturn(Optional.of(updated)); // New data after update/eviction
        when(repository.getSupportResponsesByUserId(userId))
            .thenReturn(Collections.singletonList(original))
            .thenReturn(Collections.singletonList(updated)); // New list after update/eviction

        // 2. Prime both caches
        service.getSupportResponse(id);
        service.getSupportResponsesByUserId(userId);
        verify(repository, times(1)).getById(id);
        verify(repository, times(1)).getSupportResponsesByUserId(userId);

        // 3. Perform the update (should evict both caches)
        doNothing().when(repository).update(any(SupportResponse.class));
        service.updateSupportResponse(updated);
        
        // 4. Check caches (should force repository hits)
        service.getSupportResponse(id);
        service.getSupportResponsesByUserId(userId);

        // Verification: Repository should be called twice for both cached methods
        verify(repository, times(2)).getById(id);
        verify(repository, times(2)).getSupportResponsesByUserId(userId);
    }

    @Test
    void deleteSupportResponse_shouldEvictIdAndListCaches() {
        clearCaches();
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        SupportResponse response = new SupportResponse(id, userId, "To Delete", UUID.randomUUID(), Instant.now(), Instant.now());

        // 1. Set up mocks for cache priming
        when(repository.getById(id))
            .thenReturn(Optional.of(response))
            .thenReturn(Optional.empty()); // After deletion/eviction
        when(repository.getSupportResponsesByUserId(userId))
            .thenReturn(Collections.singletonList(response))
            .thenReturn(Collections.emptyList()); // After deletion/eviction

        // 2. Prime both caches
        service.getSupportResponse(id);
        service.getSupportResponsesByUserId(userId);
        verify(repository, times(1)).getById(id);
        verify(repository, times(1)).getSupportResponsesByUserId(userId);

        // 3. Perform the delete (should evict both caches)
        doNothing().when(repository).delete(id);
        service.deleteSupportResponse(id);
        
        // 4. Check caches (should force repository hits)
        assertThrows(EntityNotFoundException.class, () -> service.getSupportResponse(id));
        service.getSupportResponsesByUserId(userId);

        // Verification: Repository should be called twice for both cached methods
        verify(repository, times(2)).getById(id);
        verify(repository, times(2)).getSupportResponsesByUserId(userId);
    }
    
    private void clearCaches() {
        if (cacheManager.getCache("supportResponseById") != null) {
            cacheManager.getCache("supportResponseById").clear();
        }
        if (cacheManager.getCache("supportResponsesByUserId") != null) {
            cacheManager.getCache("supportResponsesByUserId").clear();
        }
    }

    @Test
    void getSupportResponse_retrySucceedsOnSecondAttempt() {
        clearCaches();

        UUID id = UUID.randomUUID();
        SupportResponse response = new SupportResponse(id, UUID.randomUUID(), "Text",
                UUID.randomUUID(), Instant.now(), Instant.now());

        // 1st call -> transient failure, 2nd call -> success
        when(repository.getById(id))
                .thenThrow(new TransientDataAccessException("temp") {})
                .thenReturn(Optional.of(response));

        SupportResponse result = service.getSupportResponse(id);

        assertEquals(response.getId(), result.getId());

        // EXACT retry count: 2 total calls = 1 failure + 1 success
        verify(repository, times(2)).getById(id);
    }

    @Test
    void getSupportResponse_retryFailsAllAttempts_returns503() {
        clearCaches();

        UUID id = UUID.randomUUID();

        when(repository.getById(id))
                .thenThrow(new TransientDataAccessException("temp failure") {})
                .thenThrow(new TransientDataAccessException("temp failure") {})
                .thenThrow(new TransientDataAccessException("temp failure") {})
                .thenThrow(new TransientDataAccessException("temp failure") {});

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.getSupportResponse(id));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
        assertTrue(ex.getReason().contains(
                "The support request system is temporarily unavailable due to high load. Please try again shortly."));

        // EXACT retry count: 4 attempts (maxAttempts)
        verify(repository, times(4)).getById(id);
    }

    @Test
    void getSupportResponse_entityNotFound_shouldNotRetry() {
        clearCaches();
        UUID id = UUID.randomUUID();

        when(repository.getById(id)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.getSupportResponse(id));

        // MUST be only 1 call — NO RETRY
        verify(repository, times(1)).getById(id);
    }

    @Test
    void getSupportResponsesByUserId_retrySucceedsOnSecondAttempt() {
        clearCaches();

        UUID userId = UUID.randomUUID();
        SupportResponse r = new SupportResponse(UUID.randomUUID(), userId, "t",
                UUID.randomUUID(), Instant.now(), Instant.now());

        when(repository.getSupportResponsesByUserId(userId))
                .thenThrow(new TransientDataAccessException("temp") {})
                .thenReturn(List.of(r));

        List<SupportResponse> list = service.getSupportResponsesByUserId(userId);

        assertEquals(1, list.size());
        verify(repository, times(2)).getSupportResponsesByUserId(userId);
    }

    @Test
    void getSupportResponsesByUserId_retryFailsAllAttempts_returns503() {
        clearCaches();

        UUID userId = UUID.randomUUID();

        when(repository.getSupportResponsesByUserId(userId))
                .thenThrow(new TransientDataAccessException("temp") {})
                .thenThrow(new TransientDataAccessException("temp") {})
                .thenThrow(new TransientDataAccessException("temp") {})
                .thenThrow(new TransientDataAccessException("temp") {});

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.getSupportResponsesByUserId(userId)
        );

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
        assertTrue(ex.getReason().contains(
                "The support request system is temporarily unavailable due to high load. Please try again shortly."));

        verify(repository, times(4)).getSupportResponsesByUserId(userId);
    }

    @Test
    void writeOperations_shouldNotRetry() {
        UUID id = UUID.randomUUID();
        SupportResponse response = new SupportResponse(id, UUID.randomUUID(), "t",
                UUID.randomUUID(), Instant.now(), Instant.now());

        doThrow(new TransientDataAccessException("write fail") {})
                .when(repository).update(any());

        assertThrows(TransientDataAccessException.class, () -> service.updateSupportResponse(response));

        // Must be EXACTLY one call — never retried.
        verify(repository, times(1)).update(any());
    }
}
