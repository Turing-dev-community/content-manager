package com.dehold.contentmanager.content.customersupport.service;

import com.dehold.contentmanager.content.customersupport.model.SupportRequest;
import com.dehold.contentmanager.content.customersupport.repository.SupportRequestRepository;
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
import static org.mockito.Mockito.when;

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
}
