package com.dehold.contentmanager.user.service;

import com.dehold.contentmanager.exception.EntityNotFoundException;
import com.dehold.contentmanager.user.model.User;
import com.dehold.contentmanager.user.repository.UserRepository;
import com.dehold.contentmanager.user.web.dto.CreateUserRequest;
import com.dehold.contentmanager.user.web.dto.UpdateUserRequest;
import com.dehold.contentmanager.validation.model.ValidationPipelineModel;
import com.dehold.contentmanager.validation.service.ValidationPipelineService;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    private final ValidationPipelineService validationPipelineService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserServiceImpl(UserRepository userRepository, ValidationPipelineService validationPipelineService) {
        this.validationPipelineService = validationPipelineService;
        this.userRepository = userRepository;
    }

    @Override
    public User createUser(CreateUserRequest dto) {
        String encodedPassword = dto.getPassword();
        User user = new User(
                UUID.randomUUID(),
                dto.getAlias(),
                dto.getEmail(),
                Instant.now(),
                Instant.now(),
                dto.getUsername(),
                encodedPassword,
                true
        );
        userRepository.createUser(user);
        // Insert into Spring Security tables
        userRepository.insertAuthority(dto.getUsername(), "ROLE_USER");
        return user;
    }

    @Override
    @Cacheable(
        value = "usersById", 
        key = "#id"
    )
    public User getUser(UUID id) {
        return userRepository.getUserById(id)
                .orElseThrow(() -> EntityNotFoundException.of("User", id.toString()));
    }

    @CacheEvict(
        value = "usersById", 
        key = "#id"
    )
    @Transactional
    @Override
    public User updateUser(UUID id, UpdateUserRequest dto) {
        User existingUser = getUser(id);
        String newUserName = (dto.getUsername() != null && !dto.getUsername().equals(existingUser.getUsername()))
                ? dto.getUsername()
                : existingUser.getUsername();
        String newPassword = dto.getPassword() != null
                ? passwordEncoder.encode(dto.getPassword())
                : existingUser.getPassword();
        User updatedUser = new User(
                existingUser.getId(),
                dto.getAlias() != null ? dto.getAlias() : existingUser.getAlias(),
                dto.getEmail() != null ? dto.getEmail() : existingUser.getEmail(),
                existingUser.getCreatedAt(),
                Instant.now(),
                newUserName,
                newPassword,
                true
        );
        userRepository.updateUser(updatedUser);
        userRepository.updateAuthorityUsername(existingUser.getUsername(), newUserName);
        return updatedUser;
    }

    @CacheEvict(
        value = "usersById", 
        key = "#id"
    )
    @Override
    public void deleteUser(UUID id) {
        userRepository.deleteUser(id);
        // Delete security entries
        User user = getUser(id);
        userRepository.deleteSecurityAuthorities(user.getUsername());
    }

    public List<ValidationPipelineModel> getValidationPipelineByUserIdAndContentType(UUID userId,
                                                                                     String contentType) {
        getUser(userId); // Ensure user exists
        return validationPipelineService.findByUserIdAndContentType(userId, contentType);
    }
    
}
