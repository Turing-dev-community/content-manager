// src/test/java/com/dehold/contentmanager/content/webhook/service/WebhookServiceTest.java
package com.dehold.contentmanager.content.webhook.service;

import com.dehold.contentmanager.content.webhook.model.Webhook;
import com.dehold.contentmanager.content.webhook.repository.WebhookRepository;
import com.dehold.contentmanager.exception.EntityNotFoundException;

import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
class WebhookServiceTest {

    @MockitoBean private WebhookRepository webhookRepository;
    @Autowired private WebhookServiceImpl webhookService;
    @Autowired private CacheManager cacheManager;

    private UUID userId = UUID.randomUUID();
    private UUID webhookId = UUID.randomUUID();

    @Test
    void createWebhook_shouldCreateAndReturnWebhook() {
        String url = "https://example.com/hook";
        ArgumentCaptor<Webhook> captor = ArgumentCaptor.forClass(Webhook.class);
        doNothing().when(webhookRepository).create(captor.capture());

        Webhook result = webhookService.createWebhook(userId, url);

        assertNotNull(result.getId());
        assertEquals(userId, result.getUserId());
        assertEquals(url, result.getUrl());
        assertNotNull(result.getCreatedAt());
        assertNotNull(result.getUpdatedAt());

        Webhook captured = captor.getValue();
        assertEquals(url, captured.getUrl());
        verify(webhookRepository).create(any(Webhook.class));
    }

    @Test
    void getWebhooksByUserId_shouldReturnList() {
        Webhook w1 = createWebhook("https://a.com");
        Webhook w2 = createWebhook("https://b.com");
        when(webhookRepository.findByUserId(userId)).thenReturn(List.of(w1, w2));

        List<Webhook> result = webhookService.getWebhooksByUserId(userId);

        assertEquals(2, result.size());
        verify(webhookRepository).findByUserId(userId);
    }

    @Test
    void getWebhookById_shouldReturnWebhook_whenExists() {
        Webhook webhook = createWebhook("https://test.com");
        when(webhookRepository.findById(webhookId)).thenReturn(webhook);

        Webhook result = webhookService.getWebhookById(webhookId);

        assertEquals(webhook, result);
        verify(webhookRepository).findById(webhookId);
    }

    @Test
    void getWebhookById_shouldThrowEntityNotFoundException_whenNotExists() {
        when(webhookRepository.findById(webhookId))
            .thenThrow(new EmptyResultDataAccessException(1));

        assertThrows(EntityNotFoundException.class, () ->
            webhookService.getWebhookById(webhookId));
    }

    @Test
    void updateWebhook_shouldUpdateUrl() {
        Webhook existing = createWebhook("https://old.com");
        when(webhookRepository.findById(webhookId)).thenReturn(existing);
        ArgumentCaptor<Webhook> captor = ArgumentCaptor.forClass(Webhook.class);
        doNothing().when(webhookRepository).update(captor.capture());

        Webhook result = webhookService.updateWebhook(webhookId, "https://new.com");

        assertEquals("https://new.com", result.getUrl());
        assertNotNull(result.getUpdatedAt());
        verify(webhookRepository).update(any(Webhook.class));
    }

    @Test
    void deleteWebhook_shouldDelete() {
        Webhook webhook = createWebhook("https://delete.com");
        when(webhookRepository.findById(webhookId)).thenReturn(webhook);

        webhookService.deleteWebhook(webhookId);

        verify(webhookRepository).delete(webhookId);
    }

    @Test
    void getWebhookById_shouldCacheResultOnSecondCall() {
        clearCaches();
        Webhook webhook = createWebhook("https://cached.com");
        webhook.setId(webhookId);

        when(webhookRepository.findById(webhookId)).thenReturn(webhook);

        Webhook first = webhookService.getWebhookById(webhookId);
        Webhook second = webhookService.getWebhookById(webhookId);

        assertSame(first, second, "Should return same instance from cache");
        verify(webhookRepository, times(1)).findById(webhookId);
    }

    @Test
    void getWebhooksByUserId_shouldCacheResultOnSecondCall() {
        clearCaches();
        List<Webhook> list = List.of(createWebhook("https://a.com"), createWebhook("https://b.com"));

        when(webhookRepository.findByUserId(userId)).thenReturn(list);

        List<Webhook> first = webhookService.getWebhooksByUserId(userId);
        List<Webhook> second = webhookService.getWebhooksByUserId(userId);

        assertSame(first, second, "Should return same list from cache");
        verify(webhookRepository, times(1)).findByUserId(userId);
    }

    @Test
    void createWebhook_shouldEvictBothCaches() {
        clearCaches();
        when(webhookRepository.findByUserId(userId)).thenReturn(List.of());

        webhookService.getWebhooksByUserId(userId);
        verify(webhookRepository, times(1)).findByUserId(userId);

        doNothing().when(webhookRepository).create(any());
        webhookService.createWebhook(userId, "https://new.com");

        webhookService.getWebhooksByUserId(userId);
        verify(webhookRepository, times(2)).findByUserId(userId);
    }

    @Test
    void updateWebhook_shouldEvictBothCaches() {
        clearCaches();
        Webhook existing = createWebhook("https://old.com");
        existing.setId(webhookId);
        existing.setUserId(userId);

        Webhook updated = createWebhook("https://new.com");
        updated.setId(webhookId);
        updated.setUserId(userId);

        when(webhookRepository.findById(webhookId)).thenReturn(existing);
        when(webhookRepository.findByUserId(userId)).thenReturn(List.of(existing));

        webhookService.getWebhookById(webhookId);
        webhookService.getWebhooksByUserId(userId);

        verify(webhookRepository, times(1)).findById(webhookId);
        verify(webhookRepository, times(1)).findByUserId(userId);

        doNothing().when(webhookRepository).update(any());
        webhookService.updateWebhook(webhookId, "https://new.com");

        when(webhookRepository.findById(webhookId)).thenReturn(updated);
        when(webhookRepository.findByUserId(userId)).thenReturn(List.of(updated));

        webhookService.getWebhookById(webhookId);
        webhookService.getWebhooksByUserId(userId);

        verify(webhookRepository, times(3)).findById(webhookId);
        verify(webhookRepository, times(2)).findByUserId(userId);
    }

    @Test
    void deleteWebhook_shouldEvictBothCaches() {
        clearCaches();
        Webhook webhook = createWebhook("https://delete.com");
        webhook.setId(webhookId);
        webhook.setUserId(userId);

        when(webhookRepository.findById(webhookId)).thenReturn(webhook);
        when(webhookRepository.findByUserId(userId)).thenReturn(List.of(webhook));

        webhookService.getWebhookById(webhookId);
        webhookService.getWebhooksByUserId(userId);

        verify(webhookRepository, times(1)).findById(webhookId);
        verify(webhookRepository, times(1)).findByUserId(userId);

        doNothing().when(webhookRepository).delete(webhookId);
        webhookService.deleteWebhook(webhookId);

        when(webhookRepository.findById(webhookId)).thenThrow(new EmptyResultDataAccessException(1));
        when(webhookRepository.findByUserId(userId)).thenReturn(List.of());

        assertThrows(EntityNotFoundException.class, () -> webhookService.getWebhookById(webhookId));
        webhookService.getWebhooksByUserId(userId);

        verify(webhookRepository, times(2)).findById(webhookId);
        verify(webhookRepository, times(2)).findByUserId(userId);
    }
    
    private Webhook createWebhook(String url) {
        Webhook w = new Webhook();
        w.setId(webhookId);
        w.setUserId(userId);
        w.setUrl(url);
        w.setCreatedAt(Instant.now());
        w.setUpdatedAt(Instant.now());
        return w;
    }

    private void clearCaches() {
        Optional.ofNullable(cacheManager.getCache("webhookById")).ifPresent(cache -> cache.clear());
        Optional.ofNullable(cacheManager.getCache("webhooksByUser")).ifPresent(cache -> cache.clear());
    }

}