package com.dehold.contentmanager.content.productoffer.model;

import com.dehold.contentmanager.content.Content;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class ProductOffer implements Content {

    private UUID id;
    private UUID userId;

    private String title;
    private String description;
    private String brand;
    private String category;

    private BigDecimal originalPrice;
    private BigDecimal offerPrice;
    private Integer discountPercentage;

    private Integer stockQuantity;
    private String deliveryTime;
    private boolean isActive = true;

    private Instant createdAt;
    private Instant updatedAt;

    // === Constructors ===
    public ProductOffer() {}

    public ProductOffer(UUID id, UUID userId, String title, String description, String brand,
                        String category, BigDecimal originalPrice, BigDecimal offerPrice,
                        Integer discountPercentage, Integer stockQuantity, String deliveryTime,
                        boolean isActive, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.description = description;
        this.brand = brand;
        this.category = category;
        this.originalPrice = originalPrice;
        this.offerPrice = offerPrice;
        this.discountPercentage = discountPercentage;
        this.stockQuantity = stockQuantity;
        this.deliveryTime = deliveryTime;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // === Getters & Setters ===
    @Override
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    @Override
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public BigDecimal getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(BigDecimal originalPrice) { this.originalPrice = originalPrice; }

    public BigDecimal getOfferPrice() { return offerPrice; }
    public void setOfferPrice(BigDecimal offerPrice) { this.offerPrice = offerPrice; }

    public Integer getDiscountPercentage() { return discountPercentage; }
    public void setDiscountPercentage(Integer discountPercentage) { this.discountPercentage = discountPercentage; }

    public Integer getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }

    public String getDeliveryTime() { return deliveryTime; }
    public void setDeliveryTime(String deliveryTime) { this.deliveryTime = deliveryTime; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}