package com.dehold.contentmanager.content.productoffer.repository;

import com.dehold.contentmanager.content.productoffer.model.ProductOffer;
import com.dehold.contentmanager.user.model.User;
import com.dehold.contentmanager.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ProductOfferRepositoryIntegrationTest {

    @Autowired
    private ProductOfferRepository productOfferRepository;

    @Autowired
    private UserRepository userRepository;

    private String uniqueUsername() {
        return "TestUser-" + UUID.randomUUID();
    }

    @Test
    void shouldCreateAndRetrieveSingleOffer_verifyAllFields() {
        // Arrange
        User user = createUniqueUser("all-fields-test@example.com");

        ProductOffer offer = new ProductOffer(
            UUID.randomUUID(),
            user.getId(),
            "MacBook Pro M4",
            "14-inch, 16GB RAM, 512GB SSD",
            "Apple",
            "Laptops",
            new BigDecimal("199900.00"),
            new BigDecimal("179900.00"),
            10,
            25,
            "Free delivery in 1 day",
            true,
            Instant.now(),
            Instant.now()
        );

        // Act
        productOfferRepository.create(offer);
        List<ProductOffer> result = productOfferRepository.findByUserId(user.getId());
        ProductOffer retrieved = result.get(0);

        // Assert — ALL FIELDS EXPLICITLY VERIFIED
        assertNotNull(retrieved.getId());
        assertEquals(user.getId(), retrieved.getUserId());

        assertEquals("MacBook Pro M4", retrieved.getTitle());
        assertEquals("14-inch, 16GB RAM, 512GB SSD", retrieved.getDescription());
        assertEquals("Apple", retrieved.getBrand());
        assertEquals("Laptops", retrieved.getCategory());

        assertEquals(new BigDecimal("199900.00"), retrieved.getOriginalPrice());
        assertEquals(new BigDecimal("179900.00"), retrieved.getOfferPrice());
        assertEquals(Integer.valueOf(10), retrieved.getDiscountPercentage());

        assertEquals(Integer.valueOf(25), retrieved.getStockQuantity());
        assertEquals("Free delivery in 1 day", retrieved.getDeliveryTime());
        assertTrue(retrieved.isActive());

        assertNotNull(retrieved.getCreatedAt());
        assertNotNull(retrieved.getUpdatedAt());
    }

    @Test
    void shouldRetrieveMultipleOffersForUser() {
        User user = createUniqueUser("multiple-offers@example.com");
        productOfferRepository.create(createSampleOffer(user.getId()));
        productOfferRepository.create(createSampleOffer(user.getId(), "Pixel 9", new BigDecimal("89900.00")));

        List<ProductOffer> offers = productOfferRepository.findByUserId(user.getId());
        assertEquals(2, offers.size());
    }

    @Test
    void shouldUpdateOffer_verifyMutableFields() {
        User user = createUniqueUser("update-test@example.com");
        ProductOffer offer = createSampleOffer(user.getId());

        productOfferRepository.create(offer);

        // Update key fields
        offer.setTitle("Updated Title");
        offer.setOfferPrice(new BigDecimal("99900.00"));
        offer.setDiscountPercentage(25);
        offer.setStockQuantity(5);
        offer.setDeliveryTime("Next week");
        offer.setActive(false);

        productOfferRepository.update(offer);

        ProductOffer updated = productOfferRepository.findById(offer.getId());

        assertEquals("Updated Title", updated.getTitle());
        assertEquals(new BigDecimal("99900.00"), updated.getOfferPrice());
        assertEquals(Integer.valueOf(25), updated.getDiscountPercentage());
        assertEquals(Integer.valueOf(5), updated.getStockQuantity());
        assertEquals("Next week", updated.getDeliveryTime());
        assertFalse(updated.isActive());
    }

    @Test
    void shouldDeleteOffer() {
        User user = createUniqueUser("delete-offer@example.com");
        ProductOffer offer = createSampleOffer(user.getId());
        productOfferRepository.create(offer);

        productOfferRepository.delete(offer.getId());
        List<ProductOffer> result = productOfferRepository.findByUserId(user.getId());

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnEmptyListForNoOffers() {
        User user = createUniqueUser("no-offers@example.com");
        List<ProductOffer> offers = productOfferRepository.findByUserId(user.getId());
        assertTrue(offers.isEmpty());
    }

    // === Helper Methods ===
    private User createUniqueUser(String email) {
        User user = new User(UUID.randomUUID(), "offeruser", email, Instant.now(), Instant.now(), uniqueUsername(), "TestUser-" + UUID.randomUUID(), true);
        userRepository.createUser(user);
        return user;
    }

    private ProductOffer createSampleOffer(UUID userId) {
        return createSampleOffer(userId, "iPhone 15 Pro", new BigDecimal("119900.00"));
    }

    private ProductOffer createSampleOffer(UUID userId, String title, BigDecimal offerPrice) {
        return new ProductOffer(
            UUID.randomUUID(), userId, title, "Latest flagship", "Apple", "Smartphones",
            new BigDecimal("129900.00"), offerPrice, 8, 50, "2-3 days", true,
            Instant.now(), Instant.now()
        );
    }
}