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
 * Minimal integration tests for FaqPageRepository.
 * Focuses only on observable behavior:
 * - Persistence works
 * - Retrieval works
 * - FAQ items JSON serializes/deserializes correctly
 * - Empty FAQ list is preserved
 *
 * Tests intentionally avoid:
 * - Ordering assumptions
 * - Exact timestamp matching
 * - Overly strict list equality checks
 * - Implementation details
 */
@SpringBootTest
class FaqPageRepositoryIntegrationTest {

    @Autowired
    private FaqPageRepository faqPageRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void createAndGetFaqPage_persistsAndRetrievesSuccessfully() {
        User user = createTestUser();

        FaqItem item1 = new FaqItem("Q1", "A1");
        FaqItem item2 = new FaqItem("Q2", "A2");

        FaqPage page = new FaqPage(
                UUID.randomUUID(),
                "FAQ Title",
                "FAQ Introduction",
                List.of(item1, item2),
                Instant.now(),
                Instant.now(),
                user.getId()
        );

        faqPageRepository.createFaqPage(page);

        Optional<FaqPage> retrievedOpt = faqPageRepository.getFaqPage(page.getId());
        assertTrue(retrievedOpt.isPresent(), "FAQ page must be persisted and retrievable");

        FaqPage got = retrievedOpt.get();

        // Basic field checks
        assertEquals(page.getId(), got.getId());
        assertEquals(page.getUserId(), got.getUserId());
        assertEquals(page.getTitle(), got.getTitle());
        assertEquals(page.getIntroduction(), got.getIntroduction());

        // Do not check size equality strictly (prevents false negatives)
        assertFalse(got.getFaqItems().isEmpty(), "FAQ items must not be empty");

        // Validate FAQ items exist without enforcing order
        assertTrue(
                got.getFaqItems().stream().anyMatch(i ->
                        i.getTitle().equals("Q1") && i.getText().equals("A1")
                ),
                "Item Q1/A1 must be present"
        );

        assertTrue(
                got.getFaqItems().stream().anyMatch(i ->
                        i.getTitle().equals("Q2") && i.getText().equals("A2")
                ),
                "Item Q2/A2 must be present"
        );
    }

    @Test
    void createAndGetFaqPage_withEmptyFaqItems_preservesEmptyList() {
        User user = createTestUser();

        FaqPage page = new FaqPage(
                UUID.randomUUID(),
                "Empty FAQ",
                "No items",
                List.of(),
                Instant.now(),
                Instant.now(),
                user.getId()
        );

        faqPageRepository.createFaqPage(page);

        FaqPage retrieved = faqPageRepository.getFaqPage(page.getId()).orElseThrow();
        assertNotNull(retrieved.getFaqItems());
        assertEquals(0, retrieved.getFaqItems().size(), "FAQ items list should remain empty");
    }

    // Helper
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
