package com.dehold.contentmanager.rateLimiter.controller;

import com.dehold.contentmanager.ContentManagerApplicationTests;
import com.dehold.contentmanager.exception.CustomErrorResponse;
import com.dehold.contentmanager.ratelimiter.config.RateLimitConfig;
import com.dehold.contentmanager.ratelimiter.service.RateLimitConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


public class RateLimitAdminControllerTest extends ContentManagerApplicationTests {

    private static final String API_PATH = "/api/admin/rate-limits";

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    // We inject the service layer here primarily for cleanup purposes,
    // although all core CRUD operations are performed via the API calls.
    @Autowired
    private RateLimitConfigService rateLimitConfigService;

    private String getUrl() {
        return "http://localhost:" + port + API_PATH;
    }

    // TestRestTemplate configured with Admin credentials
    private TestRestTemplate adminRestTemplate;

    // TestRestTemplate configured with non-Admin/User credentials
    private TestRestTemplate unauthorizedRestTemplate;


    @BeforeEach
    void setup() {
        // Assuming "admin:adminpass" provides ROLE_ADMIN rights
        this.adminRestTemplate = restTemplate.withBasicAuth("admin", "adminpass");

        // Assuming "user:userpass" provides a valid login but lacks ROLE_ADMIN rights
        this.unauthorizedRestTemplate = restTemplate.withBasicAuth("user", "userpass");
    }

    // --- Security Tests ---

    @Test
    void testAdminApi_unauthenticated_thenReturns401() {
        // Use the base restTemplate without authentication
        ResponseEntity<CustomErrorResponse> response = restTemplate.getForEntity(getUrl(), CustomErrorResponse.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void testAdminApi_unauthorizedUser_thenReturns403() {
        // Use the restTemplate authenticated as a non-Admin user
        ResponseEntity<CustomErrorResponse> response = unauthorizedRestTemplate.getForEntity(getUrl(), CustomErrorResponse.class);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void testAdminApi_adminUser_canAccessGetAll() {
        // Use the restTemplate authenticated as an Admin user
        ResponseEntity<List<RateLimitConfig>> response = adminRestTemplate.exchange(
                getUrl(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<RateLimitConfig>>() {}
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        // Check if the response body is a list (could be empty or contain defaults)
        assertDoesNotThrow(() -> response.getBody().size());
    }

    // --- CRUD Tests ---

    @Test
    void testCreateReadDeleteFlow_Success() {
        // 1. CREATE
        // Updated constructor call: public RateLimitConfig(UUID id, String pathPattern, long capacity, long refillTokens, long refillIntervalMillis, Instant createdAt, Instant updatedAt)
        RateLimitConfig newConfig = new RateLimitConfig(
                null, "/api/int-test/**", 15, 5, 1000L, null, null
        );

        ResponseEntity<RateLimitConfig> createResponse = adminRestTemplate.postForEntity(
                getUrl(),
                newConfig,
                RateLimitConfig.class
        );

        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        RateLimitConfig createdConfig = createResponse.getBody();
        assertNotNull(createdConfig);
        assertNotNull(createdConfig.getId());
        assertEquals(newConfig.getPathPattern(), createdConfig.getPathPattern());
        // Verify the server populated the timestamps
        assertNotNull(createdConfig.getCreatedAt());
        assertNotNull(createdConfig.getUpdatedAt());

        // 2. READ (Verify it exists)
        String readUrl = getUrl() + "/" + createdConfig.getId();
        ResponseEntity<RateLimitConfig> readResponse = adminRestTemplate.getForEntity(
                readUrl,
                RateLimitConfig.class
        );

        assertEquals(HttpStatus.OK, readResponse.getStatusCode());
        assertEquals(createdConfig.getId(), readResponse.getBody().getId());

        // 3. UPDATE
        // The payload for update should include the ID, but the timestamps are usually ignored or overwritten by the server.
        RateLimitConfig updatePayload = new RateLimitConfig(
                createdConfig.getId(), createdConfig.getPathPattern(), 30, 10, 5000L, null, null
        );

        HttpEntity<RateLimitConfig> updateEntity = new HttpEntity<>(updatePayload);
        ResponseEntity<RateLimitConfig> updateResponse = adminRestTemplate.exchange(
                readUrl,
                HttpMethod.PUT,
                updateEntity,
                RateLimitConfig.class
        );

        assertEquals(HttpStatus.OK, updateResponse.getStatusCode());
        assertEquals(30, updateResponse.getBody().getCapacity());

        // 4. DELETE (Verify deletion and cache reload)
        ResponseEntity<Void> deleteResponse = adminRestTemplate.exchange(
                readUrl,
                HttpMethod.DELETE,
                null,
                Void.class
        );

        assertEquals(HttpStatus.NO_CONTENT, deleteResponse.getStatusCode());

        // 5. READ (Verify it's gone)
        ResponseEntity<RateLimitConfig> readDeletedResponse = adminRestTemplate.getForEntity(
                readUrl,
                RateLimitConfig.class
        );

        assertEquals(HttpStatus.NOT_FOUND, readDeletedResponse.getStatusCode());
    }

    // --- Validation Test ---

    @Test
    void testCreateConfig_InvalidInput_thenReturns400() {
        RateLimitConfig invalidConfig = new RateLimitConfig(
                null, "", 0, 0, 0L, null, null // Invalid Path, Capacity=0, Refill=0
        );

        ResponseEntity<CustomErrorResponse> response = adminRestTemplate.postForEntity(
                getUrl(),
                invalidConfig,
                CustomErrorResponse.class
        );

        // Expecting Bad Request (400) due to validation failure
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());

        // Check for specific validation errors
        String errorString = response.getBody().toString();
        assertTrue(errorString.contains("pathPattern: must not be empty"), "Should reject empty pathPattern");
        assertTrue(errorString.contains("capacity: must be greater than 0"), "Should reject capacity <= 0");
    }
}
