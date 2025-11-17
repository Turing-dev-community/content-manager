package com.dehold.contentmanager.content.customersupport.web;

import com.dehold.contentmanager.content.customersupport.model.SupportResponse;
import com.dehold.contentmanager.content.customersupport.repository.SupportResponseRepository;
import com.dehold.contentmanager.content.customersupport.web.dto.CreateSupportResponseRequest;
import com.dehold.contentmanager.content.customersupport.web.dto.UpdateSupportResponseRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SupportResponseControllerIntegrationTest {
    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private SupportResponseRepository repository;

    @Test
    void createSupportResponse_shouldReturnCreated() {
        CreateSupportResponseRequest request = new CreateSupportResponseRequest();
        request.setText("Support response text");
        request.setSupportRequest(UUID.randomUUID());
        ResponseEntity<SupportResponse> response = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/support-responses",
            request,
            SupportResponse.class
        );
        assertEquals(201, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(request.getText(), response.getBody().getText());
        assertEquals(request.getSupportRequest(), response.getBody().getSupportRequest());
    }

    @Test
    void getSupportResponse_shouldReturnResponse() {
        SupportResponse response = new SupportResponse(UUID.randomUUID(), UUID.randomUUID(), "Test text", UUID.randomUUID(), Instant.now(), Instant.now());
        repository.create(response);
        ResponseEntity<SupportResponse> getResponse = restTemplate.getForEntity(
            "http://localhost:" + port + "/api/support-responses/" + response.getId(),
            SupportResponse.class
        );
        assertEquals(200, getResponse.getStatusCode().value());
        assertNotNull(getResponse.getBody());
        assertEquals(response.getText(), getResponse.getBody().getText());
        assertEquals(response.getSupportRequest(), getResponse.getBody().getSupportRequest());
    }

    @Test
    void updateSupportResponse_shouldReturnUpdated() {
        SupportResponse response = new SupportResponse(UUID.randomUUID(), UUID.randomUUID(), "Old text", UUID.randomUUID(), Instant.now(), Instant.now());
        repository.create(response);
        UpdateSupportResponseRequest request = new UpdateSupportResponseRequest();
        request.setText("Updated text");
        request.setSupportRequest(response.getSupportRequest());
        HttpEntity<UpdateSupportResponseRequest> entity = new HttpEntity<>(request);
        ResponseEntity<SupportResponse> updateResponse = restTemplate.exchange(
            "http://localhost:" + port + "/api/support-responses/" + response.getId(),
            HttpMethod.PUT,
            entity,
            SupportResponse.class
        );
        assertEquals(200, updateResponse.getStatusCode().value());
        assertNotNull(updateResponse.getBody());
        assertEquals(request.getText(), updateResponse.getBody().getText());
    }

    @Test
    void deleteSupportResponse_shouldDelete() {
        SupportResponse response = new SupportResponse(UUID.randomUUID(), UUID.randomUUID(), "Delete text", UUID.randomUUID(), Instant.now(), Instant.now());
        repository.create(response);
        restTemplate.delete("http://localhost:" + port + "/api/support-responses/" + response.getId());
        ResponseEntity<SupportResponse> getResponse = restTemplate.getForEntity(
            "http://localhost:" + port + "/api/support-responses/" + response.getId(),
            SupportResponse.class
        );
        assertEquals(404, getResponse.getStatusCode().value());
    }

    @Test
    void getSupportResponse_shouldReturnNotFound() {
        UUID nonExistentId = UUID.randomUUID();

        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/api/support-responses/" + nonExistentId,
            String.class
        );

        assertEquals(404, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("The entity SupportResponse with id " + nonExistentId + " does not exist"));
    }
}
