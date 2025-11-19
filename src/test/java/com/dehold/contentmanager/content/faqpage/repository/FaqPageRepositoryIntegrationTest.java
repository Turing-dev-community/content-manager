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

    @Test
    void createAndGetFaqPage_withLargeFaqItemsList() {
        User user = createTestUser();
        List<FaqItem> items = List.of(
                new FaqItem("Q1", "A1"), new FaqItem("Q2", "A2"), new FaqItem("Q3", "A3"),
                new FaqItem("Q4", "A4"), new FaqItem("Q5", "A5"), new FaqItem("Q6", "A6"),
                new FaqItem("Q7", "A7"), new FaqItem("Q8", "A8"), new FaqItem("Q9", "A9"),
                new FaqItem("Q10", "A10")
        );
        FaqPage page = new FaqPage(UUID.randomUUID(), "Large FAQ", "Many items", items, Instant.now(), Instant.now(), user.getId());

        faqPageRepository.createFaqPage(page);
        FaqPage retrieved = faqPageRepository.getFaqPage(page.getId()).orElseThrow();

        assertEquals(10, retrieved.getFaqItems().size());
    }

    @Test
    void createAndGetFaqPage_withSpecialCharacters() {
        User user = createTestUser();
        String title = "FAQ: \"Quotes\" & 'Apostrophes'";
        String intro = "Contains <html> tags & special chars: ñ, é, ü";
        FaqItem item = new FaqItem("What about \"this\"?", "Use 'escaping' & special chars!");
        FaqPage page = new FaqPage(UUID.randomUUID(), title, intro, List.of(item), Instant.now(), Instant.now(), user.getId());

        faqPageRepository.createFaqPage(page);
        FaqPage retrieved = faqPageRepository.getFaqPage(page.getId()).orElseThrow();

        assertEquals(title, retrieved.getTitle());
        assertEquals(intro, retrieved.getIntroduction());
        assertEquals(1, retrieved.getFaqItems().size());
    }

    @Test
    void getFaqPagesByUserId_withMultiplePages_returnsAll() {
        User user = createTestUser();
        FaqPage page1 = new FaqPage(UUID.randomUUID(), "FAQ 1", "Intro 1", List.of(new FaqItem("Q1", "A1")), Instant.now(), Instant.now(), user.getId());
        FaqPage page2 = new FaqPage(UUID.randomUUID(), "FAQ 2", "Intro 2", List.of(new FaqItem("Q2", "A2")), Instant.now(), Instant.now(), user.getId());
        FaqPage page3 = new FaqPage(UUID.randomUUID(), "FAQ 3", "Intro 3", List.of(new FaqItem("Q3", "A3")), Instant.now(), Instant.now(), user.getId());

        faqPageRepository.createFaqPage(page1);
        faqPageRepository.createFaqPage(page2);
        faqPageRepository.createFaqPage(page3);

        List<FaqPage> pages = faqPageRepository.getFaqPagesByUserId(user.getId());

        assertEquals(3, pages.size());
        
        // Validate all pages exist without ordering assumptions
        assertTrue(pages.stream().anyMatch(p -> p.getId().equals(page1.getId())));
        assertTrue(pages.stream().anyMatch(p -> p.getId().equals(page2.getId())));
        assertTrue(pages.stream().anyMatch(p -> p.getId().equals(page3.getId())));
    }

    @Test
    void getFaqPagesByUserId_withNoPages_returnsEmptyList() {
        User user = createTestUser();
        List<FaqPage> pages = faqPageRepository.getFaqPagesByUserId(user.getId());
        assertEquals(0, pages.size());
    }

    @Test
    void getFaqPagesByUserId_returnOnlyUserPages() {
        User user1 = createTestUser();
        User user2 = createTestUser();

        FaqPage page1 = new FaqPage(UUID.randomUUID(), "User 1 FAQ", "I1", List.of(), Instant.now(), Instant.now(), user1.getId());
        FaqPage page2 = new FaqPage(UUID.randomUUID(), "User 1 FAQ 2", "I2", List.of(), Instant.now(), Instant.now(), user1.getId());
        FaqPage page3 = new FaqPage(UUID.randomUUID(), "User 2 FAQ", "I3", List.of(), Instant.now(), Instant.now(), user2.getId());

        faqPageRepository.createFaqPage(page1);
        faqPageRepository.createFaqPage(page2);
        faqPageRepository.createFaqPage(page3);

        List<FaqPage> user1Pages = faqPageRepository.getFaqPagesByUserId(user1.getId());

        assertEquals(2, user1Pages.size());
        assertTrue(user1Pages.stream().allMatch(p -> p.getUserId().equals(user1.getId())));
    }

    @Test
    void getFaqPagesByUserId_eachPageHasValidStructure() {
        User user = createTestUser();
        FaqPage page1 = new FaqPage(UUID.randomUUID(), "FAQ 1", "Intro 1", List.of(new FaqItem("Q", "A")), Instant.now(), Instant.now(), user.getId());
        FaqPage page2 = new FaqPage(UUID.randomUUID(), "FAQ 2", "Intro 2", List.of(new FaqItem("Q2", "A2")), Instant.now(), Instant.now(), user.getId());

        faqPageRepository.createFaqPage(page1);
        faqPageRepository.createFaqPage(page2);

        List<FaqPage> pages = faqPageRepository.getFaqPagesByUserId(user.getId());

        for (FaqPage page : pages) {
            assertNotNull(page.getId());
            assertNotNull(page.getTitle());
            assertNotNull(page.getIntroduction());
            assertNotNull(page.getUserId());
            assertNotNull(page.getCreatedAt());
            assertNotNull(page.getUpdatedAt());
            assertNotNull(page.getFaqItems());
        }
    }

    @Test
    void deleteUser_cascadesAndDeletesAssociatedFaqPages() {
        User user = createTestUser();
        FaqPage page1 = new FaqPage(UUID.randomUUID(), "FAQ 1", "Will be deleted", List.of(new FaqItem("Q", "A")), Instant.now(), Instant.now(), user.getId());
        FaqPage page2 = new FaqPage(UUID.randomUUID(), "FAQ 2", "Also deleted", List.of(new FaqItem("Q2", "A2")), Instant.now(), Instant.now(), user.getId());

        faqPageRepository.createFaqPage(page1);
        faqPageRepository.createFaqPage(page2);

        // Verify pages exist
        List<FaqPage> beforeDelete = faqPageRepository.getFaqPagesByUserId(user.getId());
        assertEquals(2, beforeDelete.size());

        // Delete user (should cascade delete FAQ pages)
        userRepository.deleteUser(user.getId());

        // Verify pages are gone
        List<FaqPage> afterDelete = faqPageRepository.getFaqPagesByUserId(user.getId());
        assertEquals(0, afterDelete.size());
        
        // Verify direct lookup also returns empty
        assertFalse(faqPageRepository.getFaqPage(page1.getId()).isPresent());
        assertFalse(faqPageRepository.getFaqPage(page2.getId()).isPresent());
    }

    @Test
    void createFaqPage_preservesUserIdCorrectly() {
        User user = createTestUser();
        UUID expectedUserId = user.getId();
        FaqPage page = new FaqPage(UUID.randomUUID(), "FAQ", "Intro", List.of(), Instant.now(), Instant.now(), expectedUserId);

        faqPageRepository.createFaqPage(page);
        FaqPage retrieved = faqPageRepository.getFaqPage(page.getId()).orElseThrow();

        assertEquals(expectedUserId, retrieved.getUserId());
    }

    @Test
    void createFaqPage_setsPersistenceTimestamps() {
        User user = createTestUser();
        FaqPage page = new FaqPage(UUID.randomUUID(), "FAQ", "Intro", List.of(), Instant.now(), Instant.now(), user.getId());

        faqPageRepository.createFaqPage(page);
        FaqPage retrieved = faqPageRepository.getFaqPage(page.getId()).orElseThrow();

        assertNotNull(retrieved.getCreatedAt());
        assertNotNull(retrieved.getUpdatedAt());
    }

    @Test
    void createMultiplePages_eachHasUniqueId() {
        User user = createTestUser();
        FaqPage page1 = new FaqPage(UUID.randomUUID(), "FAQ 1", "I1", List.of(), Instant.now(), Instant.now(), user.getId());
        FaqPage page2 = new FaqPage(UUID.randomUUID(), "FAQ 2", "I2", List.of(), Instant.now(), Instant.now(), user.getId());
        FaqPage page3 = new FaqPage(UUID.randomUUID(), "FAQ 3", "I3", List.of(), Instant.now(), Instant.now(), user.getId());

        faqPageRepository.createFaqPage(page1);
        faqPageRepository.createFaqPage(page2);
        faqPageRepository.createFaqPage(page3);

        List<FaqPage> pages = faqPageRepository.getFaqPagesByUserId(user.getId());
        
        assertEquals(3, pages.size());
        long uniqueIds = pages.stream().map(FaqPage::getId).distinct().count();
        assertEquals(3, uniqueIds);
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
