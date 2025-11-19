package com.dehold.contentmanager.content.productoffer.web.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class CreateProductOfferRequest {
    private String title;
    private String description;
    private String brand;
    private String category;
    private BigDecimal originalPrice;
    private BigDecimal offerPrice;
    private Integer discountPercentage;
    private Integer stockQuantity;
    private String deliveryTime;
    private UUID userId;

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
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
}