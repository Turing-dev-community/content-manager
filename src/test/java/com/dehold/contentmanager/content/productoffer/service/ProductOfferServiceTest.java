// src/test/java/com/dehold/contentmanager/content/productoffer/service/ProductOfferServiceTest.java
package com.dehold.contentmanager.content.productoffer.service;

import com.dehold.contentmanager.content.productoffer.model.ProductOffer;
import com.dehold.contentmanager.content.productoffer.repository.ProductOfferRepository;
import com.dehold.contentmanager.exception.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
class ProductOfferServiceTest {

    @MockitoBean
    private ProductOfferRepository productOfferRepository;

    @Autowired
    private ProductOfferServiceImpl productOfferService;

    @Autowired
    private CacheManager cacheManager;

    private UUID offerId;
    private UUID userId;

    @BeforeEach
    void setUp() {
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

    @Test
    void getProductOffer_shouldCacheResultOnSecondCall() {
        clearCaches();
        ProductOffer offer = createValidOffer();
        offer.setId(offerId);

        when(productOfferRepository.findById(offerId)).thenReturn(offer);

        ProductOffer first = productOfferService.getProductOffer(offerId);
        ProductOffer second = productOfferService.getProductOffer(offerId);

        assertSame(first, second, "Should return same instance from cache");
        verify(productOfferRepository, times(1)).findById(offerId);
    }

    @Test
    void getAllProductOffers_shouldCacheResultOnSecondCall() {
        clearCaches();
        List<ProductOffer> list = List.of(createValidOffer());

        when(productOfferRepository.findAll()).thenReturn(list);

        List<ProductOffer> first = productOfferService.getAllProductOffers();
        List<ProductOffer> second = productOfferService.getAllProductOffers();

        assertSame(first, second, "Should return same list from cache");
        verify(productOfferRepository, times(1)).findAll();
    }

    @Test
    void getProductOffersByUserId_shouldCacheResultOnSecondCall() {
        clearCaches();
        List<ProductOffer> list = List.of(createValidOffer());

        when(productOfferRepository.findByUserId(userId)).thenReturn(list);

        List<ProductOffer> first = productOfferService.getProductOffersByUserId(userId);
        List<ProductOffer> second = productOfferService.getProductOffersByUserId(userId);

        assertSame(first, second, "Should return same list from cache");
        verify(productOfferRepository, times(1)).findByUserId(userId);
    }

    @Test
    void createProductOffer_shouldEvictAllCaches() {
        clearCaches();
        when(productOfferRepository.findAll()).thenReturn(List.of());
        when(productOfferRepository.findByUserId(userId)).thenReturn(List.of());

        productOfferService.getAllProductOffers();
        productOfferService.getProductOffersByUserId(userId);

        verify(productOfferRepository, times(1)).findAll();
        verify(productOfferRepository, times(1)).findByUserId(userId);

        ProductOffer newOffer = createValidOffer();
        newOffer.setId(null);
        doNothing().when(productOfferRepository).create(any());

        productOfferService.createProductOffer(newOffer);

        productOfferService.getAllProductOffers();
        productOfferService.getProductOffersByUserId(userId);

        verify(productOfferRepository, times(2)).findAll();
        verify(productOfferRepository, times(2)).findByUserId(userId);
    }

    @Test
    void updateProductOffer_shouldEvictAllCaches() {
        clearCaches();
        ProductOffer existing = createValidOffer();
        existing.setId(offerId);
        existing.setUserId(userId);

        ProductOffer updated = createValidOffer();
        updated.setId(offerId);
        updated.setUserId(userId);
        updated.setTitle("Updated iPhone");

        when(productOfferRepository.findById(offerId))
                .thenReturn(existing) // Call 1: Priming getProductOffer
                .thenReturn(existing) // Call 2: Internal read in updateProductOffer (before update)
                .thenReturn(updated);  // Call 3: Post-eviction getProductOffer

        when(productOfferRepository.findAll()).thenReturn(List.of(existing));
        when(productOfferRepository.findByUserId(userId)).thenReturn(List.of(existing));

        productOfferService.getProductOffer(offerId);
        productOfferService.getAllProductOffers();
        productOfferService.getProductOffersByUserId(userId);

        verify(productOfferRepository, times(1)).findById(offerId);
        verify(productOfferRepository, times(1)).findAll();
        verify(productOfferRepository, times(1)).findByUserId(userId);

        doNothing().when(productOfferRepository).update(any());
        productOfferService.updateProductOffer(offerId, updated);

        when(productOfferRepository.findAll()).thenReturn(List.of(updated));
        when(productOfferRepository.findByUserId(userId)).thenReturn(List.of(updated));

        // Cache miss → repo called again (Calls 3 to repo, plus new calls for list/user)
        productOfferService.getProductOffer(offerId);
        productOfferService.getAllProductOffers();
        productOfferService.getProductOffersByUserId(userId);

        // Final verification: findById should be called 3 times now
        verify(productOfferRepository, times(3)).findById(offerId); // <-- FIX HERE
        verify(productOfferRepository, times(2)).findAll();
        verify(productOfferRepository, times(2)).findByUserId(userId);
    }
    
    @Test
    void deleteProductOffer_shouldEvictAllCaches() {
        clearCaches();
        ProductOffer offer = createValidOffer();
        offer.setId(offerId);
        offer.setUserId(userId);

        when(productOfferRepository.findById(offerId)).thenReturn(offer);
        when(productOfferRepository.findAll()).thenReturn(List.of(offer));
        when(productOfferRepository.findByUserId(userId)).thenReturn(List.of(offer));

        productOfferService.getProductOffer(offerId);
        productOfferService.getAllProductOffers();
        productOfferService.getProductOffersByUserId(userId);

        verify(productOfferRepository, times(1)).findById(offerId);
        verify(productOfferRepository, times(1)).findAll();
        verify(productOfferRepository, times(1)).findByUserId(userId);

        doNothing().when(productOfferRepository).delete(offerId);
        productOfferService.deleteProductOffer(offerId);

        when(productOfferRepository.findById(offerId)).thenThrow(new EmptyResultDataAccessException(1));
        when(productOfferRepository.findAll()).thenReturn(List.of());
        when(productOfferRepository.findByUserId(userId)).thenReturn(List.of());

        assertThrows(EntityNotFoundException.class, () -> productOfferService.getProductOffer(offerId));
        productOfferService.getAllProductOffers();
        productOfferService.getProductOffersByUserId(userId);

        verify(productOfferRepository, times(2)).findById(offerId);
        verify(productOfferRepository, times(2)).findAll();
        verify(productOfferRepository, times(2)).findByUserId(userId);
    }

    private void clearCaches() {
        Optional.ofNullable(cacheManager.getCache("productOfferById")).ifPresent(cache -> cache.clear());
        Optional.ofNullable(cacheManager.getCache("productOffersAll")).ifPresent(cache -> cache.clear());
        Optional.ofNullable(cacheManager.getCache("productOffersByUser")).ifPresent(cache -> cache.clear());
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