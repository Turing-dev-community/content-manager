// src/main/java/com/dehold/contentmanager/content/productoffer/service/ProductOfferService.java
package com.dehold.contentmanager.content.productoffer.service;

import com.dehold.contentmanager.content.productoffer.model.ProductOffer;

import java.util.List;
import java.util.UUID;

public interface ProductOfferService {

    ProductOffer createProductOffer(ProductOffer offer);

    ProductOffer getProductOffer(UUID id);

    List<ProductOffer> getAllProductOffers();

    List<ProductOffer> getProductOffersByUserId(UUID userId);

    ProductOffer updateProductOffer(UUID id, ProductOffer offer);

    void deleteProductOffer(UUID id);
}