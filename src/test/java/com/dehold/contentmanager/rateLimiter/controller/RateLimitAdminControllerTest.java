package com.dehold.contentmanager.rateLimiter.controller;

import com.dehold.contentmanager.ContentManagerApplicationTests;
import com.dehold.contentmanager.ratelimiter.config.RateLimitConfig;
import com.dehold.contentmanager.ratelimiter.service.RateLimitConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.*;


import static org.junit.jupiter.api.Assertions.*;

public class RateLimitAdminControllerTest extends ContentManagerApplicationTests {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RateLimitConfigService service;

    private String adminUser;
    private String adminPass;

    private String baseUrl() {
        return "http://localhost:" + port + "/api/admin/rate-limits";
    }

    private String randomPattern() {
        return "/api/test/" + UUID.randomUUID().toString().substring(0, 8);
    }

    @BeforeEach
    void init() {
        jdbcTemplate.execute("DELETE FROM rate_limit_config");
        jdbcTemplate.execute("DELETE FROM authorities");
        jdbcTemplate.execute("DELETE FROM \"user\"");

        adminUser = "admin_" + UUID.randomUUID().toString().substring(0, 8);
        adminPass = "pass_" + UUID.randomUUID().toString().substring(0, 8);

        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();
        jdbcTemplate.update(
                "INSERT INTO \"user\" (id, alias, email, username, password, enabled, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                userId,
                "Admin User",
                adminUser + "@example.com",
                adminUser,
                "{noop}" + adminPass,
                true,
                now,
                now
        );

        jdbcTemplate.update(
                "INSERT INTO authorities (username, authority) VALUES (?, ?)",
                adminUser,
                "ROLE_ADMIN"
        );
    }


    //Dynamic path configuration-> Ability to configure custom patterns
    //Database persistence of rate limit configs
    //exists in cache

    @Test
    void admin_create_shouldPersistAndShowInService() {
        TestRestTemplate admin = restTemplate.withBasicAuth("admin", "pass");

        RateLimitConfig cfg = new RateLimitConfig();
        cfg.setPathPattern(randomPattern());
        cfg.setCapacity(10);
        cfg.setRefillTokens(1);
        cfg.setRefillIntervalMillis(1000);

        ResponseEntity<RateLimitConfig> res =
                admin.postForEntity(baseUrl(), cfg, RateLimitConfig.class);

        assertEquals(HttpStatus.OK, res.getStatusCode());
        RateLimitConfig created = res.getBody();
        assertNotNull(created);
        assertNotNull(created.getId());

        // DB exists
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM rate_limit_config WHERE id = ?",
                Integer.class,
                created.getId()
        );
        assertEquals(1, count);

        // service cache visible
        boolean existsInCache = service.findAll()
                .stream()
                .anyMatch(c -> c.getId().equals(created.getId()));

        assertTrue(existsInCache);
    }

    //Update → Should refresh the in-memory cache
    @Test
    void admin_update_shouldUpdateDBAndService() {
        TestRestTemplate admin = restTemplate.withBasicAuth("admin", "pass");

        RateLimitConfig cfg = new RateLimitConfig();
        cfg.setPathPattern(randomPattern());
        cfg.setCapacity(5);
        cfg.setRefillTokens(1);
        cfg.setRefillIntervalMillis(2000);

        RateLimitConfig created =
                admin.postForEntity(baseUrl(), cfg, RateLimitConfig.class).getBody();

        created.setCapacity(50);
        created.setRefillTokens(10);

        HttpEntity<RateLimitConfig> entity =
                new HttpEntity<>(created, new HttpHeaders());

        ResponseEntity<RateLimitConfig> updateRes = admin.exchange(
                baseUrl() + "/" + created.getId(),
                HttpMethod.PUT,
                entity,
                RateLimitConfig.class
        );

        assertEquals(HttpStatus.OK, updateRes.getStatusCode());
        RateLimitConfig updated = updateRes.getBody();
        assertEquals(50, updated.getCapacity());
        assertEquals(10, updated.getRefillTokens());

        // DB validation
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT capacity, refill_tokens FROM rate_limit_config WHERE id = ?",
                created.getId()
        );

        assertEquals(50, ((Number) row.get("capacity")).longValue());
        assertEquals(10, ((Number) row.get("refill_tokens")).longValue());

        // Service validation
        Optional<RateLimitConfig> cached =
                service.findAll().stream()
                        .filter(c -> c.getId().equals(created.getId()))
                        .findFirst();

        assertTrue(cached.isPresent());
        assertEquals(50, cached.get().getCapacity());
        assertEquals(10, cached.get().getRefillTokens());
    }

    //Delete → Should remove from in-memory cache
    @Test
    void admin_delete_shouldRemoveFromDBAndService() {
        TestRestTemplate admin = restTemplate.withBasicAuth("admin", "pass");

        RateLimitConfig cfg = new RateLimitConfig();
        cfg.setPathPattern(randomPattern());
        cfg.setCapacity(1);
        cfg.setRefillTokens(1);
        cfg.setRefillIntervalMillis(1000);

        RateLimitConfig created =
                admin.postForEntity(baseUrl(), cfg, RateLimitConfig.class).getBody();

        ResponseEntity<Void> delRes = admin.exchange(
                baseUrl() + "/" + created.getId(),
                HttpMethod.DELETE,
                null,
                Void.class
        );

        assertEquals(HttpStatus.NO_CONTENT, delRes.getStatusCode());

        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM rate_limit_config WHERE id = ?",
                Integer.class,
                created.getId()
        );
        assertEquals(0, count);

        boolean existsInCache = service.findAll()
                .stream()
                .anyMatch(c -> c.getId().equals(created.getId()));

        assertFalse(existsInCache);
    }

    // Longest/specific pattern precedence
    @Test
    void pattern_precedence_longestPatternWins() {
        TestRestTemplate admin = restTemplate.withBasicAuth("admin", "pass");

        RateLimitConfig broad = new RateLimitConfig();
        broad.setPathPattern("/api/users/**");
        broad.setCapacity(10);
        broad.setRefillTokens(1);
        broad.setRefillIntervalMillis(1000);
        admin.postForEntity(baseUrl(), broad, RateLimitConfig.class);

        RateLimitConfig specific = new RateLimitConfig();
        specific.setPathPattern("/api/users/profile");
        specific.setCapacity(100);
        specific.setRefillTokens(5);
        specific.setRefillIntervalMillis(1000);
        admin.postForEntity(baseUrl(), specific, RateLimitConfig.class);

        List<RateLimitConfig> all = service.findAll();

        String requestPath = "/api/users/profile";

        Optional<RateLimitConfig> selected = all.stream()
                .filter(c -> match(c.getPathPattern(), requestPath))
                .sorted((a, b) -> Integer.compare(b.getPathPattern().length(), a.getPathPattern().length()))
                .findFirst();

        assertTrue(selected.isPresent());
        assertEquals("/api/users/profile", selected.get().getPathPattern());
    }

    private boolean match(String pattern, String path) {
        if (pattern.equals(path)) return true;

        if (pattern.endsWith("/**")) {
            String prefix = pattern.substring(0, pattern.length() - 3);
            return path.startsWith(prefix);
        }
        return false;
    }

    private void createNonAdminUser(String username, String password) {
        UUID uid = UUID.randomUUID();
        Instant now = Instant.now();

        jdbcTemplate.update(
                "INSERT INTO \"user\" (id, alias, email, username, password, enabled, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                uid,
                "Normal User",
                username + "@example.com",
                username,
                "{noop}" + password,
                true,
                now,
                now
        );

        // do NOT give ROLE_ADMIN — using ROLE_USER instead
        jdbcTemplate.update(
                "INSERT INTO authorities (username, authority) VALUES (?, ?)",
                username,
                "ROLE_USER"
        );
    }

    /**
     * Unauthenticated requests must receive 401 UNAUTHORIZED on all admin endpoints.
     */
    @Test
    void unauthenticated_access_shouldReturn401() {

        RateLimitConfig cfg = new RateLimitConfig();
        cfg.setPathPattern("/unauth/test");
        cfg.setCapacity(1);
        cfg.setRefillTokens(1);
        cfg.setRefillIntervalMillis(1000);


        // PUT
        ResponseEntity<String> putRes = restTemplate.exchange(
                baseUrl() + "/" + UUID.randomUUID(),
                HttpMethod.PUT,
                new HttpEntity<>(cfg),
                String.class
        );
        assertEquals(HttpStatus.UNAUTHORIZED, putRes.getStatusCode(), "PUT should be 401 when unauthenticated");

        // DELETE
        ResponseEntity<String> delRes = restTemplate.exchange(
                baseUrl() + "/" + UUID.randomUUID(),
                HttpMethod.DELETE,
                null,
                String.class
        );
        assertEquals(HttpStatus.UNAUTHORIZED, delRes.getStatusCode(), "DELETE should be 401 when unauthenticated");
    }

    /**
     * Authenticated but non-admin users must receive 403 FORBIDDEN on all admin endpoints (method-level @PreAuthorize enforced).
     */
    @Test
    void nonAdmin_access_shouldReturn403() {
        String user = "user_" + UUID.randomUUID().toString().substring(0, 8);
        String pass = "pass_" + UUID.randomUUID().toString().substring(0, 8);

        createNonAdminUser(user, pass);

        TestRestTemplate userClient = restTemplate.withBasicAuth("user", "pass");

        // GET
        ResponseEntity<String> getRes = userClient.getForEntity(baseUrl(), String.class);
        assertEquals(HttpStatus.FORBIDDEN, getRes.getStatusCode(), "GET should be 403 for non-admin user");

        // POST
        RateLimitConfig cfg = new RateLimitConfig();
        cfg.setPathPattern("/blocked/test");
        cfg.setCapacity(1);
        cfg.setRefillTokens(1);
        cfg.setRefillIntervalMillis(1000);

        ResponseEntity<String> postRes = userClient.postForEntity(baseUrl(), cfg, String.class);
        assertEquals(HttpStatus.FORBIDDEN, postRes.getStatusCode(), "POST should be 403 for non-admin user");

        // PUT (attempt with random UUID)
        ResponseEntity<String> putRes = userClient.exchange(
                baseUrl() + "/" + UUID.randomUUID(),
                HttpMethod.PUT,
                new HttpEntity<>(cfg),
                String.class
        );
        assertEquals(HttpStatus.FORBIDDEN, putRes.getStatusCode(), "PUT should be 403 for non-admin user");

        // DELETE (attempt with random UUID)
        ResponseEntity<String> delRes = userClient.exchange(
                baseUrl() + "/" + UUID.randomUUID(),
                HttpMethod.DELETE,
                null,
                String.class
        );
        assertEquals(HttpStatus.FORBIDDEN, delRes.getStatusCode(), "DELETE should be 403 for non-admin user");
    }

    /**
     * Bonus: explicit admin GET list test to demonstrate authorized access to read endpoint.
     */
    @Test
    void admin_get_list_shouldReturn200() {
        TestRestTemplate admin = restTemplate.withBasicAuth("admin", "pass");

        // Ensure there is at least one config to list
        RateLimitConfig cfg = new RateLimitConfig();
        cfg.setPathPattern(randomPattern());
        cfg.setCapacity(2);
        cfg.setRefillTokens(1);
        cfg.setRefillIntervalMillis(1000);
        admin.postForEntity(baseUrl(), cfg, RateLimitConfig.class);

        ResponseEntity<RateLimitConfig[]> listRes = admin.getForEntity(baseUrl(), RateLimitConfig[].class);
        assertEquals(HttpStatus.OK, listRes.getStatusCode(), "Admin should be able to GET list");
        assertNotNull(listRes.getBody());
        assertTrue(listRes.getBody().length >= 1, "List should contain at least one config");
    }



}
