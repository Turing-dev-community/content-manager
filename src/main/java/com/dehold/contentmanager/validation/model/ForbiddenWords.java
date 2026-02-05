package com.dehold.contentmanager.validation.model;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.UUID;

public class ForbiddenWords {
    UUID id;
    UUID userId;
    String description;
    String contentType;
    String fieldName;
    LinkedHashSet<String> words;
    Instant createdAt;
    Instant updatedAt;

    public ForbiddenWords(UUID id, UUID userId, String description, String contentType, String fieldName,
                          LinkedHashSet<String> words) {
        this.id = id;
        this.userId = userId;
        this.description = description;
        this.contentType = contentType;
        this.fieldName = fieldName;
        this.words = words;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public LinkedHashSet<String> getWords() {
        return words;
    }

    public void setWords(LinkedHashSet<String> words) {
        this.words = words;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
