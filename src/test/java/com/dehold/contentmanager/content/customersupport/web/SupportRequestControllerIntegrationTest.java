package com.dehold.contentmanager.content.customersupport.web;

import com.dehold.contentmanager.content.customersupport.model.SupportRequest;
import com.dehold.contentmanager.content.customersupport.repository.SupportRequestRepository;
import com.dehold.contentmanager.content.customersupport.web.dto.CustomerRequestDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SupportRequestControllerIntegrationTest {
    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private SupportRequestRepository repository;

    @Test
    void createCustomerRequest_shouldReturnCreated() {
        CustomerRequestDto request = new CustomerRequestDto();
        request.setText("Customer request text");
        request.setSupportResponse(UUID.randomUUID());
        request.setCustomerId(UUID.randomUUID());

        ResponseEntity<CustomerRequestDto> response = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/customer-requests",
            request,
            CustomerRequestDto.class
        );

        assertEquals(201, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(request.getText(), response.getBody().getText());
        assertNotNull(response.getBody().getId());
        assertNotNull(response.getBody().getCreatedAt());
        assertNotNull(response.getBody().getUpdatedAt());
    }

    @Test
    void getCustomerRequest_shouldReturnRequest() {
        SupportRequest request = new SupportRequest(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Test text",
            UUID.randomUUID(),
            UUID.randomUUID(),
            Instant.now(),
            Instant.now()
        );
        repository.create(request);

        ResponseEntity<CustomerRequestDto> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/api/customer-requests/" + request.getId(),
            CustomerRequestDto.class
        );

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(request.getText(), response.getBody().getText());
        assertEquals(request.getId(), response.getBody().getId());
    }

    @Test
    void updateCustomerRequest_shouldReturnUpdated() {
        SupportRequest request = new SupportRequest(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Old text",
            UUID.randomUUID(),
            UUID.randomUUID(),
            Instant.now(),
            Instant.now()
        );
        repository.create(request);

        CustomerRequestDto updateRequest = new CustomerRequestDto();
        updateRequest.setText("Updated text");
        updateRequest.setSupportResponse(request.getSupportResponse());
        updateRequest.setCustomerId(request.getCustomerId());

        HttpEntity<CustomerRequestDto> entity = new HttpEntity<>(updateRequest);
        ResponseEntity<CustomerRequestDto> response = restTemplate.exchange(
            "http://localhost:" + port + "/api/customer-requests/" + request.getId(),
            HttpMethod.PUT,
            entity,
            CustomerRequestDto.class
        );

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(updateRequest.getText(), response.getBody().getText());
        assertEquals(request.getId(), response.getBody().getId());
    }

    @Test
    void deleteCustomerRequest_shouldReturnNoContent() {
        SupportRequest request = new SupportRequest(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Test text",
            UUID.randomUUID(),
            UUID.randomUUID(),
            Instant.now(),
            Instant.now()
        );
        repository.create(request);

        ResponseEntity<Void> response = restTemplate.exchange(
            "http://localhost:" + port + "/api/customer-requests/" + request.getId(),
            HttpMethod.DELETE,
            null,
            Void.class
        );

        assertEquals(204, response.getStatusCode().value());

        Optional<SupportRequest> deletedRequest = repository.getById(request.getId());
        assertTrue(deletedRequest.isEmpty());
    }

    @Test
    void getCustomerRequest_shouldReturnNotFound() {
        UUID nonExistentId = UUID.randomUUID();

        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/api/customer-requests/" + nonExistentId,
            String.class
        );

        assertEquals(404, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("The entity CustomerRequest with id " + nonExistentId + " does not " +
                "exist"));
    }
}
