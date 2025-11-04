package com.dehold.contentmanager.validation.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class ValidationPipelineModel {
    private UUID id;
    private UUID userId;
    private String contentType;
    private boolean isActive;
    private List<ValidationStepModel> steps;
    private Instant createdAt;

    public ValidationPipelineModel(UUID id, UUID userId, String contentType, boolean isActive, List<ValidationStepModel> steps, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.contentType = contentType;
        this.isActive = isActive;
        this.steps = steps;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
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

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
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

