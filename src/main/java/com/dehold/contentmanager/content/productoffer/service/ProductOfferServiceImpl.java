// src/main/java/com/dehold/contentmanager/content/productoffer/service/ProductOfferServiceImpl.java
package com.dehold.contentmanager.content.productoffer.service;

import com.dehold.contentmanager.content.productoffer.model.ProductOffer;
import com.dehold.contentmanager.content.productoffer.repository.ProductOfferRepository;
import com.dehold.contentmanager.exception.EntityNotFoundException;

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
    public ProductOffer createProductOffer(ProductOffer offer) {
        offer.setId(UUID.randomUUID());
        offer.setCreatedAt(Instant.now());
        offer.setUpdatedAt(Instant.now());
        productOfferRepository.create(offer);
        return offer;
    }

    @Override
    public ProductOffer getProductOffer(UUID id) {
        try {
            return productOfferRepository.findById(id);
        } catch (EmptyResultDataAccessException e) {
            throw EntityNotFoundException.of("ProductOffer", id.toString());
        }
    }

    @Override
    public List<ProductOffer> getAllProductOffers() {
        return productOfferRepository.findAll();
    }

    @Override
    public List<ProductOffer> getProductOffersByUserId(UUID userId) {
        return productOfferRepository.findByUserId(userId);
    }

    @Override
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
    public void deleteProductOffer(UUID id) {
        productOfferRepository.delete(id);
    }
}