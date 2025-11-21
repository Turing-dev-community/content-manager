package com.dehold.contentmanager.content.faqpage.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FaqPageTest {

    @Test
    void constructorAndGettersSetters() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();
        FaqItem item = new FaqItem("Q1", "A1");

        FaqPage page = new FaqPage(id, "Title", "Intro", List.of(item), now, now, userId);

        assertEquals(id, page.getId());
        assertEquals("Title", page.getTitle());
        assertEquals("Intro", page.getIntroduction());
        assertNotNull(page.getFaqItems());
        assertEquals(1, page.getFaqItems().size());
        assertEquals(userId, page.getUserId());
        assertEquals(now, page.getCreatedAt());
        assertEquals(now, page.getUpdatedAt());

        // mutate with setters
        page.setTitle("New Title");
        page.setIntroduction("New Intro");
        page.setFaqItems(List.of(new FaqItem("Q2", "A2")));

        assertEquals("New Title", page.getTitle());
        assertEquals("New Intro", page.getIntroduction());
        assertEquals(1, page.getFaqItems().size());
        assertEquals("Q2", page.getFaqItems().get(0).getTitle());
    }
}
