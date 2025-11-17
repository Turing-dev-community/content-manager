package com.dehold.contentmanager.user.service;

import com.dehold.contentmanager.user.model.User;
import com.dehold.contentmanager.user.repository.UserRepository;
import com.dehold.contentmanager.user.web.dto.CreateUserRequest;
import com.dehold.contentmanager.user.web.dto.UpdateUserRequest;
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

class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createUser_shouldCreateAndReturnUser() {
        CreateUserRequest request = new CreateUserRequest();
        request.setAlias("Test User");
        request.setEmail("test@example.com");

        User user = new User(UUID.randomUUID(), "Test User", "test@example.com", Instant.now(), Instant.now());

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
        User user = new User(userId, "Test User", "test@example.com", Instant.now(), Instant.now());

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
        User existingUser = new User(userId, "Old Name", "old@example.com", Instant.now(), Instant.now());
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
        User user = new User(userId, "Test User", "test@example.com", Instant.now(), Instant.now());

        when(userRepository.getUserById(userId)).thenReturn(Optional.of(user));
        doNothing().when(userRepository).deleteUser(userId);

        userService.deleteUser(userId);

        verify(userRepository, times(1)).deleteUser(userId);
    }
}
