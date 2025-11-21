package com.dehold.contentmanager.validation.repository;

import com.dehold.contentmanager.validation.model.ValidationResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.Mockito.*;

class ValidationResultRepositoryTest {

    @Mock
    JdbcTemplate jdbcTemplate;

    @Spy
    ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    ValidationResultRepository validationResultRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void whenValidationResultCreateCalled_thenJdbcTemplateUpdateCalledWithProperParameters() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        String contentType = "testContentType";
        boolean isValid = true;
        Instant createdAt = Instant.now();

        ValidationResult validationResult = ValidationResult.fromPersistence(id, userId, contentType, contentId,
                isValid, Collections.emptyList(), createdAt);

        validationResultRepository.create(validationResult);

        verify(jdbcTemplate, times(1)).update(
                eq("INSERT INTO validation_result (id, user_id, content_id, content_type, is_valid, errors, " +
                        "created_at, run_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"),
                eq(id),
                eq(userId),
                eq(contentId),
                eq(contentType),
                eq(isValid),
                eq("[]"),
                eq(createdAt),
                any(UUID.class)
        );
    }
}