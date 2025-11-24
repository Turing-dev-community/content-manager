package com.dehold.contentmanager.content.webhook.service;

import com.dehold.contentmanager.content.webhook.model.Webhook;
import com.dehold.contentmanager.content.webhook.repository.WebhookRepository;
import com.dehold.contentmanager.exception.EntityNotFoundException;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class WebhookServiceImpl implements WebhookService {

    private final WebhookRepository webhookRepository;

    public WebhookServiceImpl(WebhookRepository webhookRepository) {
        this.webhookRepository = webhookRepository;
    }

    @Override
    @CacheEvict(
        value = {"webhookById", "webhooksByUser"},
         allEntries = true
        )
    public Webhook createWebhook(UUID userId, String url) {
        Webhook webhook = new Webhook();
        webhook.setId(UUID.randomUUID());
        webhook.setUserId(userId);
        webhook.setUrl(url);
        webhook.setCreatedAt(Instant.now());
        webhook.setUpdatedAt(Instant.now());
        webhookRepository.create(webhook);
        return webhook;
    }

    @Override
    @Cacheable(
        value = "webhooksByUser", 
        key = "#userId"
    )
    public List<Webhook> getWebhooksByUserId(UUID userId) {
        return webhookRepository.findByUserId(userId);
    }

    @Override
    @Cacheable(
        value = "webhookById", 
        key = "#id"
    )
    public Webhook getWebhookById(UUID id) {
        try {
            return webhookRepository.findById(id);
        } catch (EmptyResultDataAccessException e) {
            throw EntityNotFoundException.of("Webhook", id.toString());
        }
    }

    @Override
    @CacheEvict(
        value = {"webhookById", "webhooksByUser"}, 
        allEntries = true
    )
    public Webhook updateWebhook(UUID id, String url) {
        Webhook webhook = getWebhookById(id);
        webhook.setUrl(url);
        webhook.setUpdatedAt(Instant.now());
        webhookRepository.update(webhook);
        return webhook;
    }

    @Override
    @CacheEvict(
        value = {"webhookById", "webhooksByUser"}, 
        allEntries = true
    )
    public void deleteWebhook(UUID id) {
        webhookRepository.delete(id);
    }
}