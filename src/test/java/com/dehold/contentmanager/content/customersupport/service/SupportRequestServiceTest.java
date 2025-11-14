package com.dehold.contentmanager.content.customersupport.service;

import com.dehold.contentmanager.content.customersupport.model.SupportRequest;
import com.dehold.contentmanager.content.customersupport.repository.SupportRequestRepository;
import com.dehold.contentmanager.exception.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SupportRequestServiceTest {

    @Mock
    private SupportRequestRepository repository;

    @InjectMocks
    private SupportRequestService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void save_shouldCallRepositoryCreate() {
        SupportRequest request = new SupportRequest(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Test text",
            UUID.randomUUID(),
            UUID.randomUUID(),
            Instant.now(),
            Instant.now()
        );

        service.save(request);

        verify(repository, times(1)).create(request);
    }

    @Test
    void findById_shouldReturnCustomerRequest() {
        UUID id = UUID.randomUUID();
        SupportRequest request = new SupportRequest(
            id,
            UUID.randomUUID(),
            "Test text",
            UUID.randomUUID(),
            UUID.randomUUID(),
            Instant.now(),
            Instant.now()
        );
        when(repository.getById(id)).thenReturn(Optional.of(request));

        SupportRequest result = service.findById(id);

        assertEquals(request, result);
        verify(repository, times(1)).getById(id);
    }

    @Test
    void findById_shouldThrowEntityNotFoundWhenIdNotExists() {
        UUID id = UUID.randomUUID();
        when(repository.getById(id)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.findById(id));
        verify(repository, times(1)).getById(id);
    }

    @Test
    void findAll_shouldReturnAllRequests() {
        List<SupportRequest> requests = new ArrayList<>();
        requests.add(new SupportRequest(UUID.randomUUID(), UUID.randomUUID(), "test", UUID.randomUUID(), UUID.randomUUID(), Instant.now(), Instant.now()));
        when(repository.findAll()).thenReturn(requests);

        List<SupportRequest> result = service.findAll();

        assertEquals(1, result.size());
        verify(repository, times(1)).findAll();
    }

    @Test
    void findByIdOptional_shouldReturnOptional() {
        UUID id = UUID.randomUUID();
        SupportRequest request = new SupportRequest(id, UUID.randomUUID(), "test", UUID.randomUUID(), UUID.randomUUID(), Instant.now(), Instant.now());
        when(repository.getById(id)).thenReturn(Optional.of(request));

        Optional<SupportRequest> result = service.findByIdOptional(id);

        assertTrue(result.isPresent());
        assertEquals(id, result.get().getId());
    }

    @Test
    void createCustomerRequest_shouldCallRepository() {
        SupportRequest request = new SupportRequest(UUID.randomUUID(), UUID.randomUUID(), "test", UUID.randomUUID(), UUID.randomUUID(), Instant.now(), Instant.now());

        service.createCustomerRequest(request);

        verify(repository, times(1)).create(request);
    }

    @Test
    void updateCustomerRequest_shouldCallRepository() {
        SupportRequest request = new SupportRequest(UUID.randomUUID(), UUID.randomUUID(), "test", UUID.randomUUID(), UUID.randomUUID(), Instant.now(), Instant.now());

        service.updateCustomerRequest(request);

        verify(repository, times(1)).update(request);
    }

    @Test
    void deleteById_shouldCallRepositoryDeleteById() {
        UUID id = UUID.randomUUID();

        service.deleteById(id);

        verify(repository, times(1)).deleteById(id);
    }

    @Test
    void addSubscriber_shouldAddUserToEmptySubscribersList() {
        UUID requestId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        SupportRequest request = new SupportRequest(requestId, UUID.randomUUID(), "test", UUID.randomUUID(), UUID.randomUUID(), Instant.now(), Instant.now());
        request.setSubscribers(null);

        when(repository.getById(requestId)).thenReturn(Optional.of(request));

        service.addSubscriber(requestId, userId);

        assertNotNull(request.getSubscribers());
        assertTrue(request.getSubscribers().contains(userId));
        verify(repository, times(1)).update(request);
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
        verify(repository, times(0)).update(request);
    }

    @Test
    void addSubscriber_shouldAddToExistingSubscribers() {
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
        verify(repository, times(1)).update(request);
    }

    @Test
    void removeSubscriber_shouldRemoveUserFromSubscribersList() {
        UUID requestId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        List<UUID> subscribers = new ArrayList<>();
        subscribers.add(userId);
        SupportRequest request = new SupportRequest(requestId, UUID.randomUUID(), "test", UUID.randomUUID(), UUID.randomUUID(), Instant.now(), Instant.now());
        request.setSubscribers(subscribers);

        when(repository.getById(requestId)).thenReturn(Optional.of(request));

        service.removeSubscriber(requestId, userId);

        assertFalse(request.getSubscribers().contains(userId));
        verify(repository, times(1)).update(request);
    }

    @Test
    void removeSubscriber_shouldDoNothingWhenSubscriberNotInList() {
        UUID requestId = UUID.randomUUID();
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        List<UUID> subscribers = new ArrayList<>();
        subscribers.add(userId1);
        SupportRequest request = new SupportRequest(requestId, UUID.randomUUID(), "test", UUID.randomUUID(), UUID.randomUUID(), Instant.now(), Instant.now());
        request.setSubscribers(subscribers);

        when(repository.getById(requestId)).thenReturn(Optional.of(request));

        service.removeSubscriber(requestId, userId2);

        assertEquals(1, request.getSubscribers().size());
        verify(repository, times(0)).update(request);
    }

    @Test
    void removeSubscriber_shouldDoNothingWhenSubscribersListIsNull() {
        UUID requestId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        SupportRequest request = new SupportRequest(requestId, UUID.randomUUID(), "test", UUID.randomUUID(), UUID.randomUUID(), Instant.now(), Instant.now());
        request.setSubscribers(null);

        when(repository.getById(requestId)).thenReturn(Optional.of(request));

        service.removeSubscriber(requestId, userId);

        verify(repository, times(0)).update(request);
    }
}
