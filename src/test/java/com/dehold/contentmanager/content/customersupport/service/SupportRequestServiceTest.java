package com.dehold.contentmanager.content.customersupport.service;

import com.dehold.contentmanager.content.customersupport.model.SupportRequest;
import com.dehold.contentmanager.content.customersupport.repository.SupportRequestRepository;
import com.dehold.contentmanager.exception.EntityNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.cache.CacheManager;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
class SupportRequestServiceTest {

    @MockitoBean
    private SupportRequestRepository repository;

    @Autowired
    private SupportRequestService service;

    @Autowired 
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void addSubscriber_shouldAddUserWhenSubscriberListIsEmpty() {
        UUID requestId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        SupportRequest request = new SupportRequest(requestId, UUID.randomUUID(), "test", UUID.randomUUID(), UUID.randomUUID(), Instant.now(), Instant.now());
        request.setSubscribers(null);

        when(repository.getById(requestId)).thenReturn(Optional.of(request));

        service.addSubscriber(requestId, userId);

        assertNotNull(request.getSubscribers());
        assertTrue(request.getSubscribers().contains(userId));
    }

    @Test
    void addSubscriber_shouldNotAddDuplicateSubscriber() {
        UUID requestId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        List<UUID> subscribers = new ArrayList<>();
        subscribers.add(userId);
        SupportRequest request = new SupportRequest(requestId, UUID.randomUUID(), "test", UUID.randomUUID(), UUID.randomUUID(), Instant.now(), Instant.now());
        request.setSubscribers(subscribers);

        when(repository.getById(requestId)).thenReturn(Optional.of(request));

        service.addSubscriber(requestId, userId);

        assertEquals(1, request.getSubscribers().size());
        assertTrue(request.getSubscribers().contains(userId));
    }

    @Test
    void addSubscriber_shouldAddNewSubscriberToExistingList() {
        UUID requestId = UUID.randomUUID();
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        List<UUID> subscribers = new ArrayList<>();
        subscribers.add(userId1);
        SupportRequest request = new SupportRequest(requestId, UUID.randomUUID(), "test", UUID.randomUUID(), UUID.randomUUID(), Instant.now(), Instant.now());
        request.setSubscribers(subscribers);

        when(repository.getById(requestId)).thenReturn(Optional.of(request));

        service.addSubscriber(requestId, userId2);

        assertEquals(2, request.getSubscribers().size());
        assertTrue(request.getSubscribers().contains(userId2));
    }

    @Test
    void removeSubscriber_shouldRemoveUserFromSubscribers() {
        UUID requestId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        List<UUID> subscribers = new ArrayList<>();
        subscribers.add(userId);
        SupportRequest request = new SupportRequest(requestId, UUID.randomUUID(), "test", UUID.randomUUID(), UUID.randomUUID(), Instant.now(), Instant.now());
        request.setSubscribers(subscribers);

        when(repository.getById(requestId)).thenReturn(Optional.of(request));

        service.removeSubscriber(requestId, userId);

        assertFalse(request.getSubscribers().contains(userId));
    }

    @Test
    void removeSubscriber_shouldHandleNullSubscribersList() {
        UUID requestId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        SupportRequest request = new SupportRequest(requestId, UUID.randomUUID(), "test", UUID.randomUUID(), UUID.randomUUID(), Instant.now(), Instant.now());
        request.setSubscribers(null);

        when(repository.getById(requestId)).thenReturn(Optional.of(request));

        // Should not throw exception
        service.removeSubscriber(requestId, userId);
        assertNull(request.getSubscribers());
    }

    @Test
    void findAll_shouldReturnAllRequests() {
        SupportRequest request1 = new SupportRequest(UUID.randomUUID(), UUID.randomUUID(), "test1", UUID.randomUUID(), UUID.randomUUID(), Instant.now(), Instant.now());
        SupportRequest request2 = new SupportRequest(UUID.randomUUID(), UUID.randomUUID(), "test2", UUID.randomUUID(), UUID.randomUUID(), Instant.now(), Instant.now());
        List<SupportRequest> expectedRequests = List.of(request1, request2);

        when(repository.findAll()).thenReturn(expectedRequests);

        List<SupportRequest> actualRequests = service.findAll();

        assertNotNull(actualRequests);
        assertEquals(2, actualRequests.size());
        assertTrue(actualRequests.containsAll(expectedRequests));
        
        verify(repository, times(1)).findAll();
    }

    @Test
    void findAll_withTransientFailure_shouldSucceedOnSecondAttempt() {

        clearCaches();

        SupportRequest request = new SupportRequest(UUID.randomUUID(), UUID.randomUUID(), "test", UUID.randomUUID(), UUID.randomUUID(), Instant.now(), Instant.now());
        List<SupportRequest> successfulResult = List.of(request);

        when(repository.findAll())
            .thenThrow(new TransientDataAccessResourceException("DB network issue 1"))
            .thenReturn(successfulResult); 

        List<SupportRequest> result = service.findAll();

        assertNotNull(result);
        assertEquals(1, result.size());
        
        verify(repository, times(2)).findAll();
    }

    @Test
    void findAll_withTransientFailure_shouldThrowServiceUnavailableAfterMaxRetries() {

        clearCaches();

        when(repository.findAll())
        .thenThrow(new TransientDataAccessResourceException("DB network issue 1"))
        .thenThrow(new TransientDataAccessResourceException("DB network issue 2"))
        .thenThrow(new TransientDataAccessResourceException("DB network issue 3"))
        .thenThrow(new TransientDataAccessResourceException("DB network issue 4"));
        
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            service.findAll(); 
        });

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatusCode());
        assertTrue(exception.getReason().contains("The support request system is temporarily unavailable due to high load. Please try again shortly."));

        verify(repository, times(4)).findAll();
    }

    @Test
    void findById_shouldReturnRequest_whenFound() {
        UUID id = UUID.randomUUID();
       
        SupportRequest expectedRequest = new SupportRequest(id, UUID.randomUUID(), "test_success", UUID.randomUUID(), UUID.randomUUID(), Instant.now(), Instant.now()); 
        
        when(repository.getById(id)).thenReturn(Optional.of(expectedRequest));

        SupportRequest actualRequest = service.findById(id);

        assertNotNull(actualRequest);
        assertEquals(id, actualRequest.getId());
        
        verify(repository, times(1)).getById(id);
    }

    @Test
    void findById_withTransientFailure_shouldSucceedOnSecondAttempt() {

        clearCaches();

        UUID id = UUID.randomUUID();
        SupportRequest successfulRequest = new SupportRequest(id, UUID.randomUUID(), "test_retry_success", UUID.randomUUID(), UUID.randomUUID(), Instant.now(), Instant.now());
        
        when(repository.getById(id))
            .thenThrow(new TransientDataAccessResourceException("DB network issue 1"))
            .thenReturn(Optional.of(successfulRequest)); 

        SupportRequest result = service.findById(id);

        assertNotNull(result);
        assertEquals(id, result.getId());

        verify(repository, times(2)).getById(id);
    }

    @Test
    void findById_withTransientFailure_shouldThrowServiceUnavailableAfterMaxRetries() {

        clearCaches();

        UUID id = UUID.randomUUID();

        when(repository.getById(id))
        .thenThrow(new TransientDataAccessResourceException("DB network issue 1"))
        .thenThrow(new TransientDataAccessResourceException("DB network issue 2"))
        .thenThrow(new TransientDataAccessResourceException("DB network issue 3"))
        .thenThrow(new TransientDataAccessResourceException("DB network issue 4"));
        
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            service.findById(id);
        });

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatusCode());
        
        assertTrue(exception.getReason().contains("The support request system is temporarily unavailable due to high load. Please try again shortly.")); 
       
        verify(repository, times(4)).getById(id);
    }

    @Test
    void findAll_shouldCacheResultOnSecondCall() {
        clearCaches(); // Required for reliable findAll cache testing

        List<SupportRequest> cachedList = List.of(
            new SupportRequest(UUID.randomUUID(), UUID.randomUUID(), "a", UUID.randomUUID(), UUID.randomUUID(), Instant.now(), Instant.now())
        );
        
        when(repository.findAll()).thenReturn(cachedList);

        List<SupportRequest> result1 = service.findAll();
        
        List<SupportRequest> result2 = service.findAll(); 

        assertEquals(1, result1.size());
       
        assertSame(result1, result2, "Second result list should be the same cached instance."); 
        
        verify(repository, times(1)).findAll();
    }

    @Test
    void findById_shouldCacheResultOnSecondCall() {
        clearCaches(); 

        UUID id = UUID.randomUUID();
        SupportRequest cachedRequest = new SupportRequest(id, UUID.randomUUID(), "cached_data", UUID.randomUUID(), UUID.randomUUID(), Instant.now(), Instant.now());
        
        when(repository.getById(id)).thenReturn(Optional.of(cachedRequest));

        SupportRequest result1 = service.findById(id);
        
        SupportRequest result2 = service.findById(id); 

        assertNotNull(result1);
       
        assertSame(result1, result2, "Second result should be the same cached instance."); 
        
        verify(repository, times(1)).getById(id);
    }

    @Test
    void createCustomerRequest_shouldEvictFindAllCache() {
        clearCaches(); 

        SupportRequest newRequest = new SupportRequest(UUID.randomUUID(), UUID.randomUUID(), "new", UUID.randomUUID(), UUID.randomUUID(), Instant.now(), Instant.now());
        List<SupportRequest> initialList = List.of();
        
        when(repository.findAll())
            .thenReturn(initialList)    
            .thenReturn(List.of(newRequest)); 

        service.findAll();
        
        service.createCustomerRequest(newRequest); 
        
        service.findAll(); 

        verify(repository, times(2)).findAll();
    }

    @Test
    void updateCustomerRequest_shouldEvictFindByIdCache() {
        clearCaches(); 

        UUID id = UUID.randomUUID();
        SupportRequest originalRequest = new SupportRequest(id, UUID.randomUUID(), "original", UUID.randomUUID(), UUID.randomUUID(), Instant.now(), Instant.now());
        SupportRequest updatedRequest = new SupportRequest(id, UUID.randomUUID(), "UPDATED", UUID.randomUUID(), UUID.randomUUID(), Instant.now(), Instant.now());

        when(repository.getById(id))
            .thenReturn(Optional.of(originalRequest)) 
            .thenReturn(Optional.of(updatedRequest));  
        
        SupportRequest result1 = service.findById(id);
        
        service.findById(id); 
        
        service.updateCustomerRequest(updatedRequest);
        
        SupportRequest result3 = service.findById(id); 

        assertEquals("original", result1.getText(), "Cache should hold original data.");
        assertEquals("UPDATED", result3.getText(), "New fetch should contain updated data.");
        
        verify(repository, times(2)).getById(id);
    }

    @Test
    void deleteById_shouldEvictFindByIdCache() {
        clearCaches(); 

        UUID id = UUID.randomUUID();
        SupportRequest requestToCache = new SupportRequest(id, UUID.randomUUID(), "will_be_deleted", UUID.randomUUID(), UUID.randomUUID(), Instant.now(), Instant.now());

        when(repository.getById(id))
            .thenReturn(Optional.of(requestToCache)) 
            .thenReturn(Optional.empty());            
        
        service.findById(id);
        
        service.deleteById(id); 
        
        assertThrows(EntityNotFoundException.class, () -> {
            service.findById(id);
        }, "After eviction, re-fetching the deleted ID should hit the DB and fail.");
        
        verify(repository, times(2)).getById(id);
    }

    private void clearCaches() {
        if (cacheManager.getCache("supportRequests") != null) {
            cacheManager.getCache("supportRequests").clear();
        }
        if (cacheManager.getCache("supportRequestById") != null) {
            cacheManager.getCache("supportRequestById").clear();
        }
    }

}
