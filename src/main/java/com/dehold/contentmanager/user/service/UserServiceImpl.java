package com.dehold.contentmanager.user.service;

import com.dehold.contentmanager.exception.EntityNotFoundException;
import com.dehold.contentmanager.user.model.User;
import com.dehold.contentmanager.user.repository.UserRepository;
import com.dehold.contentmanager.user.web.dto.CreateUserRequest;
import com.dehold.contentmanager.user.web.dto.UpdateUserRequest;
import com.dehold.contentmanager.validation.model.ValidationPipelineModel;
import com.dehold.contentmanager.validation.service.ValidationPipelineService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    private final ValidationPipelineService validationPipelineService;

    public UserServiceImpl(UserRepository userRepository, ValidationPipelineService validationPipelineService) {
        this.validationPipelineService = validationPipelineService;
        this.userRepository = userRepository;
    }

    @Override
    public User createUser(CreateUserRequest dto) {
        User user = new User(
                UUID.randomUUID(),
                dto.getAlias(),
                dto.getEmail(),
                Instant.now(),
                Instant.now()
        );
        userRepository.createUser(user);
        return user;
    }

    @Override
    public User getUser(UUID id) {
        return userRepository.getUserById(id)
                .orElseThrow(() -> EntityNotFoundException.of("User", id.toString()));
    }

    @Override
    public User updateUser(UUID id, UpdateUserRequest dto) {
        User existingUser = getUser(id);
        User updatedUser = new User(
                existingUser.getId(),
                dto.getAlias() != null ? dto.getAlias() : existingUser.getAlias(),
                dto.getEmail() != null ? dto.getEmail() : existingUser.getEmail(),
                existingUser.getCreatedAt(),
                Instant.now()
        );
        userRepository.updateUser(updatedUser);
        return updatedUser;
    }

    @Override
    public void deleteUser(UUID id) {
        userRepository.deleteUser(id);
    }

    public ValidationPipelineModel getValidationPipelineByUserIdAndContentType(UUID userId,
                                                                                       String contentType) {
        getUser(userId); // Ensure user exists
        return validationPipelineService.findByUserIdAndContentType(userId, contentType);
    }
}
