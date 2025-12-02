// src/main/java/com/dehold/contentmanager/content/productoffer/service/ProductOfferServiceImpl.java
package com.dehold.contentmanager.content.productoffer.service;

import com.dehold.contentmanager.content.productoffer.model.ProductOffer;
import com.dehold.contentmanager.content.productoffer.repository.ProductOfferRepository;
import com.dehold.contentmanager.exception.EntityNotFoundException;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ProductOfferServiceImpl implements ProductOfferService {

    private final ProductOfferRepository productOfferRepository;

    public ProductOfferServiceImpl(ProductOfferRepository productOfferRepository) {
        this.productOfferRepository = productOfferRepository;
    }

    @Override
    @CacheEvict(
        value = {"productOfferById", "productOffersAll", "productOffersByUser"}, 
        allEntries = true
    )
    public ProductOffer createProductOffer(ProductOffer offer) {
        offer.setId(UUID.randomUUID());
        offer.setCreatedAt(Instant.now());
        offer.setUpdatedAt(Instant.now());
        productOfferRepository.create(offer);
        return offer;
    }

    @Override
    @Cacheable(
        value = "productOfferById", 
        key = "#id"
    )
    public ProductOffer getProductOffer(UUID id) {
        try {
            return productOfferRepository.findById(id);
        } catch (EmptyResultDataAccessException e) {
            throw EntityNotFoundException.of("ProductOffer", id.toString());
        }
    }

    @Override
    @Cacheable("productOffersAll")
    public List<ProductOffer> getAllProductOffers() {
        return productOfferRepository.findAll();
    }

    @Override
    @Cacheable(
        value = "productOffersByUser", 
        key = "#userId"
    )
    public List<ProductOffer> getProductOffersByUserId(UUID userId) {
        return productOfferRepository.findByUserId(userId);
    }

    @Override
    @CacheEvict(
        value = {"productOfferById", "productOffersAll", "productOffersByUser"}, 
        allEntries = true
    )
    public ProductOffer updateProductOffer(UUID id, ProductOffer offer) {
        ProductOffer existing = productOfferRepository.findById(id);
        if (existing == null) {
            throw EntityNotFoundException.of("ProductOffer", id.toString());
        }

        // Update only mutable fields
        existing.setTitle(offer.getTitle());
        existing.setDescription(offer.getDescription());
        existing.setBrand(offer.getBrand());
        existing.setCategory(offer.getCategory());
        existing.setOriginalPrice(offer.getOriginalPrice());
        existing.setOfferPrice(offer.getOfferPrice());
        existing.setDiscountPercentage(offer.getDiscountPercentage());
        existing.setStockQuantity(offer.getStockQuantity());
        existing.setDeliveryTime(offer.getDeliveryTime());
        existing.setActive(offer.isActive());
        existing.setUpdatedAt(Instant.now());

        productOfferRepository.update(existing);
        return existing;
    }

    @Override
    @CacheEvict(
        value = {"productOfferById", "productOffersAll", "productOffersByUser"}, 
        allEntries = true
    )
    public void deleteProductOffer(UUID id) {
        productOfferRepository.delete(id);
    }
}