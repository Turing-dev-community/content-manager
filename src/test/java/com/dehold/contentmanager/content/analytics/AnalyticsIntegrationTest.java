package com.dehold.contentmanager.content.analytics;

import com.dehold.contentmanager.ContentManagerApplicationTests;
import com.dehold.contentmanager.analytics.model.ApiAccessLog;
import com.dehold.contentmanager.analytics.repository.ApiAccessLogRepository;
import com.dehold.contentmanager.content.customersupport.web.dto.CreateSupportResponseRequest;
import com.dehold.contentmanager.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AnalyticsIntegrationTest extends ContentManagerApplicationTests {

    private static final UUID userId = UUID.fromString("06c4f0e4-20d7-4886-841b-ebe0ca3622a5");

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ApiAccessLogRepository apiAccessLogRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanupDbAndSetupUser() {
        jdbcTemplate.update("DELETE FROM api_access_log");
        jdbcTemplate.update("DELETE FROM \"user\"");

        jdbcTemplate.update("INSERT INTO \"user\" (id, alias, email, username, password, enabled, created_at, updated_at) VALUES (?, ?, ?, ?, ?, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                userId, "testuser", "testuser@example.com", "TestUser-" + UUID.randomUUID(), "TestPassword-" + UUID.randomUUID());
    }

    @Test
    void givenSingleApiCall_whenGetUser_thenAccessLogIsStored() {
        restTemplate.getForEntity(
                "http://localhost:" + port + "/api/users/" + userId,
                User.class
        );

        List<ApiAccessLog> logs = apiAccessLogRepository.findAll();

        assertEquals(1, logs.size());
        ApiAccessLog log = logs.getFirst();
        assertNotNull(log.getId());
        assertEquals("/api/users/{id}", log.getUrl());
        assertNotNull(log.getTimestamp());
    }

    @Test
    void givenMultipleApiCalls_whenDifferentEndpoints_thenAllAccessLogsAreStored() {
        restTemplate.getForEntity(
                "http://localhost:" + port + "/api/users/" + userId,
                User.class
        );

        restTemplate.getForEntity(
                "http://localhost:" + port + "/api/users/" + userId,
                User.class
        );

        CreateSupportResponseRequest request = new CreateSupportResponseRequest();
        request.setText("Test response");
        request.setSupportRequest(UUID.randomUUID());

        restTemplate.postForEntity(
                "http://localhost:" + port + "/api/support-responses",
                request,
                String.class
        );

        List<ApiAccessLog> logs = apiAccessLogRepository.findAll();

        assertEquals(3, logs.size());

        long userEndpointCount = logs.stream()
                .filter(log -> log.getUrl().equals("/api/users/{id}"))
                .count();
        assertEquals(2, userEndpointCount);

        long supportResponseEndpointCount = logs.stream()
                .filter(log -> log.getUrl().equals("/api/support-responses"))
                .count();
        assertEquals(1, supportResponseEndpointCount);
    }

    @Test
    void givenMultipleCallsToSameEndpoint_whenDifferentIds_thenLogsShowSamePattern() {
        UUID secondUserId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO \"user\" (id, alias, email, username, password, enabled, created_at, updated_at) VALUES (?, ?, ?, ?, ?, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                secondUserId, "testuser2", "testuser2@example.com", "TestUser-" + UUID.randomUUID(), "TestPassword-" + UUID.randomUUID());

        restTemplate.getForEntity(
                "http://localhost:" + port + "/api/users/" + userId,
                User.class
        );

        restTemplate.getForEntity(
                "http://localhost:" + port + "/api/users/" + secondUserId,
                User.class
        );

        List<ApiAccessLog> logs = apiAccessLogRepository.findAll();

        assertEquals(2, logs.size());

        assertTrue(logs.stream().allMatch(log -> log.getUrl().equals("/api/users/{id}")));

        logs.forEach(log -> {
            assertNotNull(log.getId());
            assertNotNull(log.getTimestamp());
        });
    }

    @Test
    void givenApiCallWithNonExistentResource_whenRequest_thenAccessLogIsStillStored() {
        UUID nonExistentId = UUID.randomUUID();

        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/users/" + nonExistentId,
                String.class
        );

        List<ApiAccessLog> logs = apiAccessLogRepository.findAll();

        assertEquals(1, logs.size());
        ApiAccessLog log = logs.getFirst();
        assertEquals("/api/users/{id}", log.getUrl());
        assertNotNull(log.getTimestamp());
    }

    @Test
    void givenNoApiCalls_whenCheckingLogs_thenRepositoryIsEmpty() {
        List<ApiAccessLog> logs = apiAccessLogRepository.findAll();

        assertEquals(0, logs.size());
    }
}
