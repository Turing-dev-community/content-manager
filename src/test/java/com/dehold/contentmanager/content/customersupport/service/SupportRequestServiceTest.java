package com.dehold.contentmanager.content.customersupport.service;

import com.dehold.contentmanager.content.customersupport.model.SupportRequest;
import com.dehold.contentmanager.content.customersupport.repository.SupportRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
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
    void deleteById_shouldCallRepositoryDeleteById() {
        UUID id = UUID.randomUUID();

        service.deleteById(id);

        verify(repository, times(1)).deleteById(id);
    }
}
