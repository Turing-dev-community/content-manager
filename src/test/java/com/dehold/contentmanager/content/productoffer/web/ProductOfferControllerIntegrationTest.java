package com.dehold.contentmanager.content.productoffer.web;

import com.dehold.contentmanager.ContentManagerApplicationTests;
import com.dehold.contentmanager.content.productoffer.model.ProductOffer;
import com.dehold.contentmanager.content.productoffer.repository.ProductOfferRepository;
import com.dehold.contentmanager.content.productoffer.web.dto.CreateProductOfferRequest;
import com.dehold.contentmanager.content.productoffer.web.dto.UpdateProductOfferRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProductOfferControllerIntegrationTest extends ContentManagerApplicationTests {

    // Use a fixed ID known to be in the database setup
    private static final UUID fixedTestUserId = UUID.fromString("06c4f0e4-20d7-4886-841b-ebe0ca3622a5");

    @LocalServerPort 
    private int port;
    
    @Autowired 
    private TestRestTemplate restTemplate;

    @Autowired 
    private JdbcTemplate jdbcTemplate;
    
    // We only use the repository for cleanup to keep tests fast, 
    // but the actual data manipulation should be via the API.
    @Autowired 
    private ProductOfferRepository offerRepository;

    private String baseUrl;

    @BeforeEach
    void setupTestData() {
        
        offerRepository.deleteAll();
        
        jdbcTemplate.update("DELETE FROM \"user\"");
        
        jdbcTemplate.update("INSERT INTO \"user\" (id, alias, email, username, password, enabled, created_at, updated_at) VALUES (?, ?, ?, ?, ?, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                fixedTestUserId, "offer-test-user", "offer-test@example.com", "OfferUser", "TestPassword");
        
        this.baseUrl = "http://localhost:" + port + "/api/product-offers";
    }

    @Test
    void createProductOffer_shouldReturnCreatedProductOffer() {
        CreateProductOfferRequest request = createValidRequest();

        ResponseEntity<ProductOffer> response = restTemplate.postForEntity(baseUrl, request, ProductOffer.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody().getId());
        assertEquals("iPhone 15 Pro", response.getBody().getTitle());
        assertEquals(fixedTestUserId, response.getBody().getUserId());
        assertTrue(response.getBody().isActive());
    }

    @Test
    void getProductOffer_shouldReturnProductOffer() {
        ProductOffer saved = createAndSaveOffer("Galaxy S24");

        ResponseEntity<ProductOffer> response = restTemplate.getForEntity(
            baseUrl + "/" + saved.getId(), ProductOffer.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(saved.getId(), response.getBody().getId());
        assertEquals("Galaxy S24", response.getBody().getTitle());
    }

    @Test
    void getProductOffers_shouldReturnAllProductOffers() {
        createAndSaveOffer("Offer 1");
        createAndSaveOffer("Offer 2");

        ResponseEntity<ProductOffer[]> response = restTemplate.getForEntity(baseUrl, ProductOffer[].class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        // Check that at least the two created offers are present
        assertEquals(2, response.getBody().length); 
    }

    @Test
    void getProductOffers_shouldReturnProductOffersByUserId() {
        UUID otherUserId = UUID.fromString("514b7a57-39a7-4623-9db0-3fda971bf11f");
    
        jdbcTemplate.update("INSERT INTO \"user\" (id, alias, email, username, password, enabled, created_at, updated_at) VALUES (?, ?, ?, ?, ?, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                otherUserId, "other-user", "other-user@test.com", "OtherUser", "TestPassword");
        

        createAndSaveOffer("My Offer 1");
        createAndSaveOffer("My Offer 2");
        
        CreateProductOfferRequest req = createValidRequest();
        req.setUserId(otherUserId);
        req.setTitle("Other User Offer");
        restTemplate.postForEntity(baseUrl, req, ProductOffer.class);

        ResponseEntity<ProductOffer[]> response = restTemplate.getForEntity(
            baseUrl + "?userId=" + fixedTestUserId, ProductOffer[].class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().length);
        assertTrue(List.of(response.getBody()).stream()
            .allMatch(o -> o.getUserId().equals(fixedTestUserId)));
    }

    @Test
    void updateProductOffer_shouldReturnUpdatedProductOffer() {
        ProductOffer saved = createAndSaveOffer("Old Phone");

        UpdateProductOfferRequest update = new UpdateProductOfferRequest();
        update.setTitle("New Phone Pro Max");
        update.setOfferPrice(new BigDecimal("89900.00"));
        update.setStockQuantity(5);
        update.setIsActive(false);

        HttpEntity<UpdateProductOfferRequest> entity = new HttpEntity<>(update);
        ResponseEntity<ProductOffer> response = restTemplate.exchange(
            baseUrl + "/" + saved.getId(), HttpMethod.PUT, entity, ProductOffer.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("New Phone Pro Max", response.getBody().getTitle());
        assertEquals(new BigDecimal("89900.00"), response.getBody().getOfferPrice());
        assertEquals(5, response.getBody().getStockQuantity());
        assertFalse(response.getBody().isActive());
    }

    @Test
    void deleteProductOffer_shouldDeleteProductOffer() {
        ProductOffer saved = createAndSaveOffer("To Be Deleted");

        restTemplate.delete(baseUrl + "/" + saved.getId());

        ResponseEntity<ProductOffer> response = restTemplate.getForEntity(
            baseUrl + "/" + saved.getId(), ProductOffer.class);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    // === Helpers functions ===
    private CreateProductOfferRequest createValidRequest() {
        CreateProductOfferRequest req = new CreateProductOfferRequest();
        req.setTitle("iPhone 15 Pro");
        req.setDescription("Latest flagship from Apple");
        req.setBrand("Apple");
        req.setCategory("Smartphones");
        req.setOriginalPrice(new BigDecimal("129900.00"));
        req.setOfferPrice(new BigDecimal("109900.00"));
        req.setDiscountPercentage(15);
        req.setStockQuantity(50);
        req.setDeliveryTime("2-3 days");
        req.setUserId(fixedTestUserId); 
        return req;
    }

    private ProductOffer createAndSaveOffer(String title) {
        CreateProductOfferRequest req = createValidRequest();
        req.setTitle(title); 
        
        ResponseEntity<ProductOffer> response = restTemplate.postForEntity(baseUrl, req, ProductOffer.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode(), "Helper failed to create offer via API.");
        return response.getBody();
    }
    
}