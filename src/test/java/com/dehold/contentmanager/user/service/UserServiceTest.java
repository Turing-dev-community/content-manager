package com.dehold.contentmanager.user.service;

import com.dehold.contentmanager.exception.EntityNotFoundException;
import com.dehold.contentmanager.user.model.User;
import com.dehold.contentmanager.user.repository.UserRepository;
import com.dehold.contentmanager.user.web.dto.CreateUserRequest;
import com.dehold.contentmanager.user.web.dto.UpdateUserRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }


    @Test
    void createUser_shouldCreateAndReturnUser() {
        CreateUserRequest request = new CreateUserRequest();
        request.setAlias("Test User");
        request.setEmail("test@example.com");
        request.setUsername("TestUser"+ UUID.randomUUID());
        request.setPassword("TestPassword"+ UUID.randomUUID());

        User user = new User(UUID.randomUUID(), "Test User", "test@example.com", Instant.now(), Instant.now(), "TestUser"+ UUID.randomUUID(), "TestPassword"+ UUID.randomUUID(), true);

        doNothing().when(userRepository).createUser(any(User.class));

        User createdUser = userService.createUser(request);

        assertNotNull(createdUser);
        assertEquals(request.getAlias(), createdUser.getAlias());
        assertEquals(request.getEmail(), createdUser.getEmail());
        verify(userRepository, times(1)).createUser(any(User.class));
    }

    @Test
    void getUser_shouldReturnUserIfExists() {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "Test User", "test@example.com", Instant.now(), Instant.now(), "TestUser", "TestPassword", true);

        when(userRepository.getUserById(userId)).thenReturn(Optional.of(user));

        User foundUser = userService.getUser(userId);

        assertNotNull(foundUser);
        assertEquals(userId, foundUser.getId());
        assertEquals(user.getAlias(), foundUser.getAlias());
        assertEquals(user.getEmail(), foundUser.getEmail());
        verify(userRepository, times(1)).getUserById(userId);
    }

    @Test
    void updateUser_shouldUpdateAndReturnUpdatedUser() {
        UUID userId = UUID.randomUUID();
        User existingUser = new User(userId, "Old Name", "old@example.com", Instant.now(), Instant.now(), "TestUser", "TestPassword", true);
        Instant originalUpdatedAt = existingUser.getUpdatedAt();
        UpdateUserRequest request = new UpdateUserRequest();
        request.setAlias("New Name");
        request.setEmail("new@example.com");

        when(userRepository.getUserById(userId)).thenReturn(Optional.of(existingUser));
        doNothing().when(userRepository).updateUser(any(User.class));

        User updatedUser = userService.updateUser(userId, request);

        assertNotNull(updatedUser);
        assertEquals(request.getAlias(), updatedUser.getAlias());
        assertEquals(request.getEmail(), updatedUser.getEmail());
        assertTrue(updatedUser.getUpdatedAt().isAfter(originalUpdatedAt));
        verify(userRepository, times(1)).updateUser(any(User.class));
    }

    @Test
    void deleteUser_shouldDeleteUserIfExists() {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "Test User", "test@example.com", Instant.now(), Instant.now(), "TestUser", "TestPassword", true);

        when(userRepository.getUserById(userId)).thenReturn(Optional.of(user));
        doNothing().when(userRepository).deleteUser(userId);

        userService.deleteUser(userId);

        verify(userRepository, times(1)).deleteUser(userId);
    }

    // Password should be encoded before saving
    @Test
    void createUser_shouldEncodePasswordBeforeSaving() {
        CreateUserRequest request = new CreateUserRequest();
        request.setAlias("Test Alias");
        request.setEmail("test@example.com");
        request.setUsername("testuser");
        request.setPassword("plainpassword");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        userService.createUser(request);

        verify(userRepository, times(1)).createUser(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertNotNull(savedUser.getPassword());
        assertNotEquals("plainpassword", savedUser.getPassword(), "Password should be encoded");
        assertTrue(passwordEncoder.matches("plainpassword", savedUser.getPassword()), "Encoded password should match raw password");
    }

    // ✅ 2. Test that exception is thrown when user not found
    @Test
    void getUser_shouldThrowEntityNotFoundExceptionWhenUserDoesNotExist() {
        UUID randomId = UUID.randomUUID();
        when(userRepository.getUserById(randomId)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            userService.getUser(randomId);
        });

        assertEquals("The entity User with id " + randomId + " does not exist", exception.getMessage());
        verify(userRepository, times(1)).getUserById(randomId);
    }

    // Creates a new user with encrypted user and password and verifies that after user creation, passwords are not same.
    @Test
    void createUser_shouldCreateUserFieldsProperly() {
        CreateUserRequest request = new CreateUserRequest();
        request.setAlias("New Alias");
        request.setEmail("new@example.com");
        request.setUsername("newUsername");
        request.setPassword("newPassword123");

        User user = userService.createUser(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).createUser(userCaptor.capture());

        assertEquals("New Alias", user.getAlias());
        assertEquals("new@example.com", user.getEmail());
        assertEquals("newUsername", user.getUsername());
        assertNotEquals("newPassword123", user.getPassword());
        assertTrue(user.isEnabled());
    }
}