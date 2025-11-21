package com.dehold.contentmanager.user.web;

import com.dehold.contentmanager.ContentManagerApplicationTests;
import com.dehold.contentmanager.user.model.User;
import com.dehold.contentmanager.user.web.dto.CreateUserRequest;
import com.dehold.contentmanager.user.web.dto.UpdateUserRequest;
import com.dehold.contentmanager.user.web.dto.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import org.springframework.http.*;

import static org.junit.jupiter.api.Assertions.*;


@ActiveProfiles("test")
public class UserSecurityTest extends ContentManagerApplicationTests {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserDetailsService userDetailsService;

    private String uniqueUsername() {
        return "TestUser-" + UUID.randomUUID();
    }

    @BeforeEach
    void before() {
        jdbcTemplate.execute("DELETE FROM authorities");
        jdbcTemplate.execute("DELETE FROM \"user\"");
    }

    private void registerInMemoryUser(String username, String password) {
        InMemoryUserDetailsManager im = (InMemoryUserDetailsManager) userDetailsService;
        if (!im.userExists(username)) {
            im.createUser(org.springframework.security.core.userdetails.User
                    .withUsername(username)
                    .password(password)
                    .roles("USER")
                    .build());
        }
    }

    @Test
    void givenNotAuthorized_whenUserExists_then401() {

        String username = uniqueUsername();
        String password = "pass123";

        CreateUserRequest request = new CreateUserRequest();
        request.setAlias("Test");
        request.setEmail("u@" + UUID.randomUUID() + ".com");
        request.setUsername(username);
        request.setPassword(password);
        request.setEnabled(true);

        ResponseEntity<User> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/users",
                request, User.class);

        UUID userId = response.getBody().getId();

        UpdateUserRequest update = new UpdateUserRequest();
        update.setAlias("Test2");
        update.setEmail("u2@" + UUID.randomUUID() + ".com");
        update.setUsername(username); // MUST NOT CHANGE USERNAME
        update.setPassword(password);
        update.setEnabled(true);

        ResponseEntity<UserResponse> result = restTemplate.exchange(
                "http://localhost:" + port + "/api/users/" + userId,
                HttpMethod.PUT,
                new HttpEntity<>(update),
                UserResponse.class);

        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
    }

    @Test
    void givenNotAuthorized_whenDeletingUser_then401() {

        String username = uniqueUsername();
        String password = "pass123";

        CreateUserRequest request = new CreateUserRequest();
        request.setAlias("Test");
        request.setEmail("u@" + UUID.randomUUID() + ".com");
        request.setUsername(username);
        request.setPassword(password);
        request.setEnabled(true);

        ResponseEntity<User> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/users",
                request, User.class);

        UUID userId = response.getBody().getId();

        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                "http://localhost:" + port + "/api/users/" + userId,
                HttpMethod.DELETE,
                null, Void.class);

        assertEquals(HttpStatus.UNAUTHORIZED, deleteResponse.getStatusCode());
    }

    @Test
    void givenAuthorized_whenUserExists_thenUpdateTheUser() {

        String username = uniqueUsername();
        String password = "pass123";

        CreateUserRequest request = new CreateUserRequest();
        request.setAlias("Test");
        request.setEmail("u@" + UUID.randomUUID() + ".com");
        request.setUsername(username);
        request.setPassword(password);
        request.setEnabled(true);

        ResponseEntity<User> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/users",
                request, User.class);

        UUID userId = response.getBody().getId();

        registerInMemoryUser(username, password);

        TestRestTemplate authClient = restTemplate.withBasicAuth(username, password);

        UpdateUserRequest update = new UpdateUserRequest();
        update.setAlias("Updated");
        update.setEmail("upd@" + UUID.randomUUID() + ".com");
        update.setUsername(username); // MUST NOT CHANGE USERNAME
        update.setPassword(password);
        update.setEnabled(true);

        ResponseEntity<UserResponse> result = authClient.exchange(
                "http://localhost:" + port + "/api/users/" + userId,
                HttpMethod.PUT,
                new HttpEntity<>(update),
                UserResponse.class);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void givenAuthorized_whenDeletingUser_thenSuccess() {

        String username = uniqueUsername();
        String password = "pass123";

        CreateUserRequest request = new CreateUserRequest();
        request.setAlias("Test");
        request.setEmail("u@" + UUID.randomUUID() + ".com");
        request.setUsername(username);
        request.setPassword(password);
        request.setEnabled(true);

        ResponseEntity<User> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/users",
                request, User.class);

        UUID userId = response.getBody().getId();

        registerInMemoryUser(username, password);

        TestRestTemplate authClient = restTemplate.withBasicAuth(username, password);

        ResponseEntity<Void> deleteResponse = authClient.exchange(
                "http://localhost:" + port + "/api/users/" + userId,
                HttpMethod.DELETE, null, Void.class);

        assertEquals(HttpStatus.NOT_FOUND, deleteResponse.getStatusCode());

        ResponseEntity<UserResponse> after = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/users/" + userId,
                UserResponse.class);

        assertEquals(HttpStatus.NOT_FOUND, after.getStatusCode());
    }
}
