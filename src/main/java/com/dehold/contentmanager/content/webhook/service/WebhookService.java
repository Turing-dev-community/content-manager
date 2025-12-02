package com.dehold.contentmanager.content.webhook.service;

import com.dehold.contentmanager.content.webhook.model.Webhook;

import java.util.List;
import java.util.UUID;

public interface WebhookService {
    Webhook createWebhook(UUID userId, String url);
    List<Webhook> getWebhooksByUserId(UUID userId);
    Webhook getWebhookById(UUID id);
    Webhook updateWebhook(UUID id, String url);
    void deleteWebhook(UUID id);
}