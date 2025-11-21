package com.dehold.contentmanager.validation.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class ValidationResult {
    private final UUID id;
    private final UUID userId;
    private final String contentType;
    private final UUID contentId;
    private final boolean isValid;
    private final List<ValidationError> errors;
    private final Instant createdAt;
    private final UUID runId;

    private ValidationResult(String contentType, UUID contentId, UUID userId, boolean isValid,
                             List<ValidationError> errors) {
        this.contentType = contentType;
        this.contentId = contentId;
        this.userId = userId;
        this.isValid = isValid;
        this.errors = errors;
        this.id = UUID.randomUUID();
        this.createdAt = Instant.now();
        this.runId = null;
    }

    private ValidationResult(UUID id, UUID userId, String contentType, UUID contentId, boolean isValid,
                             List<ValidationError> errors, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.contentType = contentType;
        this.contentId = contentId;
        this.isValid = isValid;
        this.errors = errors;
        this.createdAt = createdAt;
        this.runId = null;
    }

    public static ValidationResult valid(String contentType, UUID contentId, UUID userId) {
        return new ValidationResult(contentType, contentId, userId, true, List.of());
    }

    public static ValidationResult invalid(String contentType, UUID contentId,
                                           UUID userId, List<ValidationError> errors) {
        return new ValidationResult(contentType, contentId, userId, false, errors);
    }

    public static ValidationResult fromPersistence(UUID id, UUID userId, String contentType, UUID contentId,
                                                   boolean isValid, List<ValidationError> errors, Instant createdAt) {
        return new ValidationResult(id, userId, contentType, contentId, isValid, errors, createdAt);
    }

    public boolean isValid() {
        return isValid;
    }

    public List<ValidationError> getErrors() {
        return errors;
    }

    public String getContentType() {
        return contentType;
    }

    public UUID getContentId() {
        return contentId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public UUID getRunId() {
        return runId;
    }
}