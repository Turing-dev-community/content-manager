// src/test/java/com/dehold/contentmanager/content/webhook/service/WebhookServiceTest.java
package com.dehold.contentmanager.content.webhook.service;

import com.dehold.contentmanager.content.webhook.model.Webhook;
import com.dehold.contentmanager.content.webhook.repository.WebhookRepository;
import com.dehold.contentmanager.exception.EntityNotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.dao.EmptyResultDataAccessException;

import java.util.List;
import java.util.UUID;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WebhookServiceTest {

    @Mock private WebhookRepository webhookRepository;
    @InjectMocks private WebhookServiceImpl webhookService;

    private UUID userId = UUID.randomUUID();
    private UUID webhookId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

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

    private Webhook createWebhook(String url) {
        Webhook w = new Webhook();
        w.setId(webhookId);
        w.setUserId(userId);
        w.setUrl(url);
        w.setCreatedAt(Instant.now());
        w.setUpdatedAt(Instant.now());
        return w;
    }
}