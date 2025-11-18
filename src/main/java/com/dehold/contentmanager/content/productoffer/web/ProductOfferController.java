package com.dehold.contentmanager.content.productoffer.web;

import com.dehold.contentmanager.content.productoffer.model.ProductOffer;
import com.dehold.contentmanager.content.productoffer.service.ProductOfferService;
import com.dehold.contentmanager.content.productoffer.web.dto.CreateProductOfferRequest;
import com.dehold.contentmanager.content.productoffer.web.dto.UpdateProductOfferRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/product-offers")
public class ProductOfferController {

    private final ProductOfferService productOfferService;

    public ProductOfferController(ProductOfferService productOfferService) {
        this.productOfferService = productOfferService;
    }

    @PostMapping
    public ResponseEntity<ProductOffer> createProductOffer(@RequestBody CreateProductOfferRequest request) {
        ProductOffer offer = new ProductOffer();
        offer.setUserId(request.getUserId());
        offer.setTitle(request.getTitle());
        offer.setDescription(request.getDescription());
        offer.setBrand(request.getBrand());
        offer.setCategory(request.getCategory());
        offer.setOriginalPrice(request.getOriginalPrice());
        offer.setOfferPrice(request.getOfferPrice());
        offer.setDiscountPercentage(request.getDiscountPercentage());
        offer.setStockQuantity(request.getStockQuantity());
        offer.setDeliveryTime(request.getDeliveryTime());
        offer.setActive(true);

        ProductOffer created = productOfferService.createProductOffer(offer);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductOffer> getProductOffer(@PathVariable UUID id) {
        ProductOffer offer = productOfferService.getProductOffer(id);
        return ResponseEntity.ok(offer);
    }

    @GetMapping
    public ResponseEntity<List<ProductOffer>> getAllProductOffers(
            @RequestParam(required = false) UUID userId) {
        List<ProductOffer> offers = userId == null
                ? productOfferService.getAllProductOffers()
                : productOfferService.getProductOffersByUserId(userId);
        return ResponseEntity.ok(offers);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductOffer> updateProductOffer(
            @PathVariable UUID id,
            @RequestBody UpdateProductOfferRequest request) {

        ProductOffer offer = new ProductOffer();
        offer.setTitle(request.getTitle());
        offer.setDescription(request.getDescription());
        offer.setBrand(request.getBrand());
        offer.setCategory(request.getCategory());
        offer.setOriginalPrice(request.getOriginalPrice());
        offer.setOfferPrice(request.getOfferPrice());
        offer.setDiscountPercentage(request.getDiscountPercentage());
        offer.setStockQuantity(request.getStockQuantity());
        offer.setDeliveryTime(request.getDeliveryTime());
        if (request.getIsActive() != null) {
            offer.setActive(request.getIsActive());
        }

        ProductOffer updated = productOfferService.updateProductOffer(id, offer);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProductOffer(@PathVariable UUID id) {
        productOfferService.deleteProductOffer(id);
        return ResponseEntity.noContent().build();
    }
}