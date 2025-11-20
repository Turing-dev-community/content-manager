package com.dehold.contentmanager.user.web;

import com.dehold.contentmanager.ContentManagerApplicationTests;
import com.dehold.contentmanager.user.model.User;

import com.dehold.contentmanager.user.web.dto.CreateUserRequest;
import com.dehold.contentmanager.user.web.dto.UpdateUserRequest;
import com.dehold.contentmanager.user.web.dto.UserResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;

@ActiveProfiles("default")
public class UserSecurityTest extends ContentManagerApplicationTests {

    @LocalServerPort
    private int port;
    @Autowired
    private TestRestTemplate restTemplate;

    private String uniqueUsername() {
        return "TestUser-" + UUID.randomUUID();
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void before() {
       // jdbcTemplate.execute("DELETE FROM authorities");
        jdbcTemplate.execute("DELETE FROM \"user\"");
    }

    @Test
    void givenNotAuthorized_whenUserExists_then401() {

        CreateUserRequest request = new CreateUserRequest();
        request.setAlias("Integration Test User");
        request.setEmail("integration-" + UUID.randomUUID() + "@example.com");
        request.setUsername(uniqueUsername());
        request.setPassword("TestUser-" + UUID.randomUUID());
        request.setEnabled(true);
        ResponseEntity<User> response = restTemplate.postForEntity("http://localhost:" + port + "/api/users", request, User.class);

        UUID userId = response.getBody().getId();

        UpdateUserRequest request1 = new UpdateUserRequest();
        request1.setAlias("Integration Test User");
        request1.setEmail("integration-" + UUID.randomUUID() + "@example.com");
        request1.setUsername(uniqueUsername());
        request1.setPassword("TestUser-" + UUID.randomUUID());
        request1.setEnabled(false);

        ResponseEntity<UserResponse> putResponse = restTemplate.exchange(
                "http://localhost:" + port + "/api/users/" + userId, HttpMethod.PUT, new HttpEntity<>(request1),
                UserResponse.class
        );

        assertEquals(HttpStatus.UNAUTHORIZED, putResponse.getStatusCode());
    }

    @Test
    void givenNotAuthorized_whenDeletingUser_then401() {

        CreateUserRequest request = new CreateUserRequest();
        request.setAlias("Integration Test User");
        request.setEmail("integration-" + UUID.randomUUID() + "@example.com");
        request.setUsername(uniqueUsername());
        request.setPassword("TestUser-" + UUID.randomUUID());
        request.setEnabled(true);
        ResponseEntity<User> response = restTemplate.postForEntity("http://localhost:" + port + "/api/users", request, User.class);

        UUID userId = response.getBody().getId();

        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                "http://localhost:" + port + "/api/users/" + userId, HttpMethod.DELETE, null,
                Void.class
        );

        assertEquals(HttpStatus.UNAUTHORIZED, deleteResponse.getStatusCode());
    }

    @Test
    void givenAuthorized_whenUserExists_thenUpdateTheUser() {
        String username = uniqueUsername();
        String password = "TestUser-" + UUID.randomUUID();
        CreateUserRequest request = new CreateUserRequest();
        request.setAlias("Integration Test User");
        request.setEmail("integration-" + UUID.randomUUID() + "@example.com");
        request.setUsername(username);
        request.setPassword(password);
        request.setEnabled(true);
        ResponseEntity<User> response = restTemplate.postForEntity("http://localhost:" + port + "/api/users", request, User.class);

        UUID userId = response.getBody().getId();

        TestRestTemplate authenticatedTemplate = restTemplate.withBasicAuth(username, password);

        UpdateUserRequest request1 = new UpdateUserRequest();
        request1.setAlias("Integration Test User");
        request1.setEmail("integration-" + UUID.randomUUID() + "@example.com");
        request1.setUsername(username);
        request1.setPassword(password);
        request1.setEnabled(true);

        ResponseEntity<UserResponse> putResponse = authenticatedTemplate.exchange(
                "http://localhost:" + port + "/api/users/" + userId, HttpMethod.PUT, new HttpEntity<>(request1),
                UserResponse.class
        );

        assertEquals(HttpStatus.OK, putResponse.getStatusCode());
    }

    @Test
    void givenAuthorized_whenDeletingUser_thenSuccess() {

        String username = uniqueUsername();
        String password = "TestUser-" + UUID.randomUUID();
        CreateUserRequest request = new CreateUserRequest();
        request.setAlias("Integration Test User");
        request.setEmail("integration-" + UUID.randomUUID() + "@example.com");
        request.setUsername(username);
        request.setPassword(password);
        request.setEnabled(true);
        ResponseEntity<User> response = restTemplate.postForEntity("http://localhost:" + port + "/api/users", request, User.class);

        UUID userId = response.getBody().getId();

        ResponseEntity<UserResponse> getResponse = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/users/" + userId,
                UserResponse.class
        );
        // After creating the user, the response code is 200 and actual email is retrieved from response.
        assertEquals(request.getEmail(), getResponse.getBody().getEmail());
        assertEquals(HttpStatus.OK, getResponse.getStatusCode());

        TestRestTemplate authenticatedTemplate = restTemplate.withBasicAuth(username, password);

        ResponseEntity<Void> deleteResponse = authenticatedTemplate.exchange(
                "http://localhost:" + port + "/api/users/" + userId, HttpMethod.DELETE, null,
                Void.class
        );

        // After deleting the user, the response code is 400 and actual email is null in response.
        ResponseEntity<UserResponse> afterDeleteGetResponse = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/users/" + userId,
                UserResponse.class
        );

        assertEquals(HttpStatus.NOT_FOUND, afterDeleteGetResponse.getStatusCode());
        assertNull(afterDeleteGetResponse.getBody().getEmail());
    }
}
