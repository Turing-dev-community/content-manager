package com.dehold.contentmanager.content.customersupport.model;

import com.dehold.contentmanager.content.Content;

import java.time.Instant;
import java.util.UUID;

public class SupportRequest implements Content {
    private UUID id;
    private UUID userId;
    private String text;
    private UUID supportResponse;
    private UUID customerId;
    private Instant createdAt;
    private Instant updatedAt;

    public SupportRequest() {}

    public SupportRequest(UUID id, UUID userId, String text, UUID supportResponse, UUID customerId, Instant createdAt,
                          Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.text = text;
        this.supportResponse = supportResponse;
        this.customerId = customerId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public UUID getUserId() {
        return userId;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public UUID getSupportResponse() {
        return supportResponse;
    }

    public void setSupportResponse(UUID supportResponse) {
        this.supportResponse = supportResponse;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
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
