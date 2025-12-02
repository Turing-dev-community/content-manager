package com.dehold.contentmanager.content.generic.model;

import com.dehold.contentmanager.content.Content;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class GenericContentModel implements Content {
    private UUID id;
    private UUID userId;
    private String type;
    private Map<String, ContentFieldValue> fieldNameToValue;
    private Instant createdAt;
    private Instant updatedAt;
    private UUID parentId;

    public GenericContentModel(UUID id, UUID userId, String type,
                               Map<String, ContentFieldValue> fieldNameToValue,
                               Instant createdAt, Instant updatedAt, UUID parentId) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.fieldNameToValue = fieldNameToValue;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.parentId = parentId;
    }

    @Override
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    @Override
    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, ContentFieldValue> getFieldNameToValue() {
        return fieldNameToValue;
    }

    public void setFieldNameToValue(Map<String, ContentFieldValue> fieldNameToValue) {
        this.fieldNameToValue = fieldNameToValue;
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

    public UUID getParentId() {
        return parentId;
    }

    public void setParentId(UUID parentId) {
        this.parentId = parentId;
    }
}
