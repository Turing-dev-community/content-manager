package com.dehold.contentmanager.content.customersupport.web.dto;

import java.time.Instant;
import java.util.UUID;

public class CustomerRequestDto {
    private UUID id;
    private String text;
    private UUID supportResponse;
    private UUID customerId;
    private Instant createdAt;
    private Instant updatedAt;

    // Getters and setters

    public UUID getId() {
        return id;
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
