package com.dehold.contentmanager.content.webhook.model;

import java.time.Instant;
import java.util.UUID;

public class Webhook {

    private UUID id;
    private UUID userId;
    private String url;
    private Instant createdAt;
    private Instant updatedAt;

    public Webhook() {}

    public Webhook(UUID id, UUID userId, String url, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.url = url;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
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