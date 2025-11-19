package com.dehold.contentmanager.content.generic.web;

import com.dehold.contentmanager.ContentManagerApplicationTests;
import com.dehold.contentmanager.content.generic.model.ContentFieldValue;
import com.dehold.contentmanager.content.generic.model.GenericContentModel;
import com.dehold.contentmanager.content.generic.model.ValueType;
import com.dehold.contentmanager.content.generic.repository.GenericModelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GenericContentControllerTest extends ContentManagerApplicationTests {

    private static final UUID user1Id = UUID.fromString("06c4f0e4-20d7-4886-841b-ebe0ca3622a5");
    private static final UUID user2Id = UUID.fromString("514b7a57-39a7-4623-9db0-3fda971bf11f");

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private GenericModelRepository genericModelRepository;

    @BeforeEach
    void cleanupDbAndSetupUsers(@Autowired JdbcTemplate jdbcTemplate) {
        jdbcTemplate.update("DELETE FROM generic_content");
        jdbcTemplate.update("DELETE FROM \"user\"");

        jdbcTemplate.update("INSERT INTO \"user\" (id, alias, email, username, password, enabled, created_at, updated_at) VALUES (?, ?, ?, ?, ?, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                user1Id, "testuser1", "testuser1@example.com", "TestUser-" + UUID.randomUUID(), "TestPassword-" + UUID.randomUUID());

        jdbcTemplate.update("INSERT INTO \"user\" (id, alias, email, username, password, enabled, created_at, updated_at) VALUES (?, ?, ?, ?, ?, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                user2Id, "testuser2", "testuser2@example.com", "TestUser-" + UUID.randomUUID(), "TestPassword-" + UUID.randomUUID());
    }

    @Test
    void createContent_shouldReturnCreatedContent() {
        GenericContentModel request = createSampleGenericContent(null, user1Id);

        ResponseEntity<GenericContentModel> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/content",
                request,
                GenericContentModel.class
        );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getId());
        assertEquals(request.getType(), response.getBody().getType());
        assertEquals(request.getUserId(), response.getBody().getUserId());
        assertEquals("Introduction to Spring Boot", response.getBody().getFieldNameToValue().get("title").getValue());
    }

    @Test
    void getContent_shouldReturnContent() {
        GenericContentModel content = createSampleGenericContent(UUID.randomUUID(), user1Id);
        genericModelRepository.save(content);

        ResponseEntity<GenericContentModel> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/content/" + content.getId(),
                GenericContentModel.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(content.getId(), response.getBody().getId());
        assertEquals(content.getType(), response.getBody().getType());
    }

    @Test
    void updateContent_shouldReturnUpdatedContent() {
        GenericContentModel content = createSampleGenericContent(UUID.randomUUID(), user2Id);
        genericModelRepository.save(content);

        Map<String, ContentFieldValue> updatedFields = new HashMap<>();
        updatedFields.put("title", new ContentFieldValue("title", ValueType.STRING, "Updated Title"));
        updatedFields.put("rating", new ContentFieldValue("rating", ValueType.DECIMAL, 5.0));

        GenericContentModel updateRequest = new GenericContentModel(
                content.getId(),
                content.getUserId(),
                content.getType(),
                updatedFields,
                content.getCreatedAt(),
                Instant.now(),
                content.getParentId()
        );

        HttpEntity<GenericContentModel> entity = new HttpEntity<>(updateRequest);
        ResponseEntity<GenericContentModel> response = restTemplate.exchange(
                "http://localhost:" + port + "/api/content/" + content.getId(),
                HttpMethod.PUT,
                entity,
                GenericContentModel.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Updated Title", response.getBody().getFieldNameToValue().get("title").getValue());
        assertEquals(5.0, response.getBody().getFieldNameToValue().get("rating").getValue());
    }

    @Test
    void deleteContent_shouldDeleteContent() {
        GenericContentModel createRequest = createSampleGenericContent(null, user1Id);

        ResponseEntity<GenericContentModel> createResponse = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/content",
                createRequest,
                GenericContentModel.class
        );

        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        assertNotNull(createResponse.getBody());

        UUID contentId = createResponse.getBody().getId();

        restTemplate.delete("http://localhost:" + port + "/api/content/" + contentId);

        ResponseEntity<GenericContentModel> getResponse = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/content/" + contentId,
                GenericContentModel.class
        );

        assertEquals(HttpStatus.NOT_FOUND, getResponse.getStatusCode());
    }

    @Test
    void getContent_shouldReturnNotFound() {
        UUID nonExistentId = UUID.randomUUID();

        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/content/" + nonExistentId,
                String.class
        );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("The entity GenericContent with id " + nonExistentId + " does not exist"));
    }

    private GenericContentModel createSampleGenericContent(UUID id, UUID userId) {
        Map<String, ContentFieldValue> fields = new HashMap<>();
        fields.put("title", new ContentFieldValue("title", ValueType.STRING, "Introduction to Spring Boot"));
        fields.put("viewCount", new ContentFieldValue("viewCount", ValueType.INTEGER, 1250));
        fields.put("rating", new ContentFieldValue("rating", ValueType.DECIMAL, 4.5));
        fields.put("published", new ContentFieldValue("published", ValueType.BOOLEAN, true));
        fields.put("description", new ContentFieldValue("description", ValueType.STRING, "A comprehensive guide"));

        return new GenericContentModel(
                id,
                userId,
                "article",
                fields,
                Instant.now(),
                Instant.now(),
                null
        );
    }
}