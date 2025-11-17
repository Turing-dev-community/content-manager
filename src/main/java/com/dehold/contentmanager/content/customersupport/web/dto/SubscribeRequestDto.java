package com.dehold.contentmanager.content.customersupport.web.dto;

import java.util.UUID;

public class SubscribeRequestDto {
    private UUID userId;

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }
}
