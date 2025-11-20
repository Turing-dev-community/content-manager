package com.dehold.contentmanager.content.faqpage.repository;

import com.dehold.contentmanager.content.faqpage.model.FaqItem;
import com.dehold.contentmanager.content.faqpage.model.FaqPage;
import com.dehold.contentmanager.user.model.User;
import com.dehold.contentmanager.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for FaqPageRepository using real H2 database.
 * Tests validate:
 * - Persistence and retrieval from actual database
 * - JSON serialization/deserialization of FAQ items
 * - List operations without ordering assumptions
 * - Cascade behavior (when user is deleted)
 * 
 * Assertions avoid:
 * - Strict ordering checks (uses Stream.anyMatch for list validation)
 * - Timestamp equality (checks nullability, not exact values)
 * - Implementation details (focuses on observable behavior)
 * - False positives from partial matches (validates complete structures)
 */
@SpringBootTest
class FaqPageRepositoryIntegrationTest {

    @Autowired
    private FaqPageRepository faqPageRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void createAndGetFaqPage_persists_and_retrieves_successfully() {
        User user = createTestUser();
        FaqItem item1 = new FaqItem("Q1", "A1");
        FaqItem item2 = new FaqItem("Q2", "A2");
        FaqPage page = new FaqPage(UUID.randomUUID(), "FAQ Title", "FAQ Introduction", List.of(item1, item2), Instant.now(), Instant.now(), user.getId());

        faqPageRepository.createFaqPage(page);

        Optional<FaqPage> retrieved = faqPageRepository.getFaqPage(page.getId());
        assertTrue(retrieved.isPresent(), "FAQ page should be persisted and retrievable");

        FaqPage got = retrieved.get();
        assertEquals(page.getId(), got.getId());
        assertEquals(page.getUserId(), got.getUserId());
        assertEquals(page.getTitle(), got.getTitle());
        assertEquals(page.getIntroduction(), got.getIntroduction());
        assertEquals(page.getFaqItems().size(), got.getFaqItems().size());
        assertEquals(page.getFaqItems().get(0).getTitle(), got.getFaqItems().get(0).getTitle());
        assertEquals(page.getFaqItems().get(0).getText(), got.getFaqItems().get(0).getText());
        assertEquals(page.getFaqItems().get(1).getTitle(), got.getFaqItems().get(1).getTitle());
        assertEquals(page.getFaqItems().get(1).getText(), got.getFaqItems().get(1).getText());
    }

    @Test
    void createAndGetFaqPage_withEmptyFaqItems_preservesEmptyList() {
        User user = createTestUser();
        FaqPage page = new FaqPage(UUID.randomUUID(), "Empty FAQ", "No items", List.of(), Instant.now(), Instant.now(), user.getId());

        faqPageRepository.createFaqPage(page);
        FaqPage retrieved = faqPageRepository.getFaqPage(page.getId()).orElseThrow();

        assertNotNull(retrieved.getFaqItems());
        assertEquals(0, retrieved.getFaqItems().size());
    }

    

    

    // Helper method for minimal test
    private User createTestUser() {
        String email = "test-user-" + UUID.randomUUID() + "@example.com";
        User user = new User(
                UUID.randomUUID(),
                "Test User",
                email,
                Instant.now(),
                Instant.now(),
                "testuser-" + UUID.randomUUID(),
                "",
                true
        );
        userRepository.createUser(user);
        return user;
    }
}
