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

    @Test
    void shouldCreateAndRetrieveSingleOffer() {
        User user = createUniqueUser("single-offer@example.com");
        ProductOffer offer = createSampleOffer(user.getId());

        productOfferRepository.create(offer);

        List<ProductOffer> result = productOfferRepository.findByUserId(user.getId());
        assertEquals(1, result.size());
        assertEquals("iPhone 15 Pro", result.get(0).getTitle());
        assertEquals(new BigDecimal("119900.00"), result.get(0).getOfferPrice());
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
    void shouldUpdateOffer() {
        User user = createUniqueUser("update-offer@example.com");
        ProductOffer offer = createSampleOffer(user.getId());
        productOfferRepository.create(offer);

        offer.setOfferPrice(new BigDecimal("109900.00"));
        offer.setStockQuantity(10);
        offer.setActive(false);
        productOfferRepository.update(offer);

        ProductOffer updated = productOfferRepository.findById(offer.getId());
        assertEquals(new BigDecimal("109900.00"), updated.getOfferPrice());
        assertEquals(10, updated.getStockQuantity());
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
        User user = new User(UUID.randomUUID(), "offeruser", email, Instant.now(), Instant.now());
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