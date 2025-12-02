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

    @Test
    void defaultConstructorAndSetters() {
        FaqPage page = new FaqPage();

        // defaults should be null
        assertNull(page.getId());
        assertNull(page.getTitle());
        assertNull(page.getIntroduction());
        assertNull(page.getFaqItems());
        assertNull(page.getCreatedAt());
        assertNull(page.getUpdatedAt());
        assertNull(page.getUserId());

        // set values
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();
        page.setId(id);
        page.setTitle("T");
        page.setIntroduction("I");
        page.setFaqItems(List.of(new FaqItem("Q", "A")));
        page.setCreatedAt(now);
        page.setUpdatedAt(now.plusSeconds(10));
        page.setUserId(userId);

        assertEquals(id, page.getId());
        assertEquals("T", page.getTitle());
        assertEquals("I", page.getIntroduction());
        assertNotNull(page.getFaqItems());
        assertEquals(1, page.getFaqItems().size());
        assertEquals(userId, page.getUserId());
    }

    @Test
    void faqItemsReferenceBehaviorAndNulls() {
        FaqPage page = new FaqPage();

        List<FaqItem> items = new java.util.ArrayList<>();
        items.add(new FaqItem("Q1", "A1"));

        page.setFaqItems(items);
        // modifying original list should reflect in page because reference is stored
        items.add(new FaqItem("Q2", "A2"));
        assertEquals(2, page.getFaqItems().size());

        // page should accept null for faqItems
        page.setFaqItems(null);
        assertNull(page.getFaqItems());
    }

    @Test
    void implementsContentInterface() {
        FaqPage page = new FaqPage();
        assertTrue(page instanceof com.dehold.contentmanager.content.Content);
    }
}
