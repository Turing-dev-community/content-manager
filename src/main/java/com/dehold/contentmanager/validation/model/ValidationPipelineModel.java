package com.dehold.contentmanager.validation.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class ValidationPipelineModel {
    private UUID id;
    private UUID userId;
    private String description;
    private String contentType;
    private List<ValidationStepModel> steps;
    private Instant createdAt;

    public ValidationPipelineModel(UUID id, UUID userId, String description, String contentType,
                                   List<ValidationStepModel> steps, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.description = description;
        this.contentType = contentType;
        this.steps = steps;
        this.createdAt = createdAt;
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

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<ValidationStepModel> getSteps() {
        return steps;
    }

    public void setSteps(List<ValidationStepModel> steps) {
        this.steps = steps;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

