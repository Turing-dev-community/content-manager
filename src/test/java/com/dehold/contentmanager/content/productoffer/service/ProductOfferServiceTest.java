// src/test/java/com/dehold/contentmanager/content/productoffer/service/ProductOfferServiceTest.java
package com.dehold.contentmanager.content.productoffer.service;

import com.dehold.contentmanager.content.productoffer.model.ProductOffer;
import com.dehold.contentmanager.content.productoffer.repository.ProductOfferRepository;
import com.dehold.contentmanager.exception.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.dao.EmptyResultDataAccessException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductOfferServiceTest {

    @Mock
    private ProductOfferRepository productOfferRepository;

    @InjectMocks
    private ProductOfferServiceImpl productOfferService;

    private UUID offerId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        offerId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    @Test
    void createProductOffer_shouldCreateAndReturnOffer() {
        ProductOffer offer = createValidOffer();
        offer.setId(null); // simulate incoming request

        ArgumentCaptor<ProductOffer> captor = ArgumentCaptor.forClass(ProductOffer.class);
        doNothing().when(productOfferRepository).create(captor.capture());

        ProductOffer result = productOfferService.createProductOffer(offer);

        assertNotNull(result.getId());
        assertNotNull(result.getCreatedAt());
        assertNotNull(result.getUpdatedAt());

        ProductOffer captured = captor.getValue();
        assertNotNull(captured.getId());
        assertEquals(offer.getTitle(), captured.getTitle());
        assertEquals(userId, captured.getUserId());

        verify(productOfferRepository).create(any(ProductOffer.class));
    }

    @Test
    void getProductOffer_shouldReturnOffer_whenExists() {
        ProductOffer offer = createValidOffer();
        when(productOfferRepository.findById(offerId)).thenReturn(offer);

        ProductOffer result = productOfferService.getProductOffer(offerId);

        assertEquals(offer, result);
        verify(productOfferRepository).findById(offerId);
    }

    @Test
    void getProductOffer_shouldThrowEntityNotFoundException_whenNotExists() {
        when(productOfferRepository.findById(offerId))
            .thenThrow(new EmptyResultDataAccessException(1));

        assertThrows(EntityNotFoundException.class, () ->
            productOfferService.getProductOffer(offerId));
    }

    @Test
    void getProductOffersByUserId_shouldReturnOffersForUser() {
        ProductOffer offer1 = createValidOffer();
        ProductOffer offer2 = createValidOffer();
        when(productOfferRepository.findByUserId(userId)).thenReturn(List.of(offer1, offer2));

        List<ProductOffer> result = productOfferService.getProductOffersByUserId(userId);

        assertEquals(2, result.size());
        verify(productOfferRepository).findByUserId(userId);
    }

    @Test
    void updateProductOffer_shouldUpdateAndReturnOffer() {
        ProductOffer existing = createValidOffer();
        existing.setTitle("Old Title");
        existing.setOfferPrice(new BigDecimal("100000.00"));

        ProductOffer update = createValidOffer();
        update.setTitle("New Title");
        update.setOfferPrice(new BigDecimal("89900.00"));
        update.setActive(false);

        when(productOfferRepository.findById(offerId)).thenReturn(existing);
        ArgumentCaptor<ProductOffer> captor = ArgumentCaptor.forClass(ProductOffer.class);
        doNothing().when(productOfferRepository).update(captor.capture());

        ProductOffer result = productOfferService.updateProductOffer(offerId, update);

        assertEquals("New Title", result.getTitle());
        assertEquals(new BigDecimal("89900.00"), result.getOfferPrice());
        assertFalse(result.isActive());
        assertNotNull(result.getUpdatedAt());

        verify(productOfferRepository).update(any(ProductOffer.class));
    }

    @Test
    void deleteProductOffer_shouldCallRepositoryDelete() {
        productOfferService.deleteProductOffer(offerId);
        verify(productOfferRepository).delete(offerId);
    }

    private ProductOffer createValidOffer() {
        ProductOffer offer = new ProductOffer();
        offer.setId(offerId);
        offer.setUserId(userId);
        offer.setTitle("iPhone 15 Pro");
        offer.setDescription("Latest iPhone");
        offer.setBrand("Apple");
        offer.setCategory("Smartphones");
        offer.setOriginalPrice(new BigDecimal("129900.00"));
        offer.setOfferPrice(new BigDecimal("109900.00"));
        offer.setDiscountPercentage(15);
        offer.setStockQuantity(50);
        offer.setDeliveryTime("2-3 days");
        offer.setActive(true);
        return offer;
    }
}