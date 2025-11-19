package com.dehold.contentmanager.content.productoffer.repository;

import com.dehold.contentmanager.content.productoffer.model.ProductOffer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class ProductOfferRepository {

    private final JdbcTemplate jdbcTemplate;

    public ProductOfferRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<ProductOffer> ROW_MAPPER = new ProductOfferRowMapper();

    private static class ProductOfferRowMapper implements RowMapper<ProductOffer> {
        @Override
        public ProductOffer mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new ProductOffer(
                UUID.fromString(rs.getString("id")),
                UUID.fromString(rs.getString("user_id")),
                rs.getString("title"),
                rs.getString("description"),
                rs.getString("brand"),
                rs.getString("category"),
                rs.getObject("original_price", BigDecimal.class),
                rs.getObject("offer_price", BigDecimal.class),
                getInteger(rs, "discount_percentage"),
                getInteger(rs, "stock_quantity"),
                rs.getString("delivery_time"),
                rs.getBoolean("is_active"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()
            );
        }

        private Integer getInteger(ResultSet rs, String column) throws SQLException {
            Object obj = rs.getObject(column);
            return obj == null ? null : ((Number) obj).intValue();
        }
    }

    // === CREATE ===
    public void create(ProductOffer offer) {
        String sql = """
            INSERT INTO product_offer (
                id, user_id, title, description, brand, category,
                original_price, offer_price, discount_percentage,
                stock_quantity, delivery_time, is_active,
                created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        jdbcTemplate.update(sql,
            offer.getId(),
            offer.getUserId(),
            offer.getTitle(),
            offer.getDescription(),
            offer.getBrand(),
            offer.getCategory(),
            offer.getOriginalPrice(),
            offer.getOfferPrice(),
            offer.getDiscountPercentage(),
            offer.getStockQuantity(),
            offer.getDeliveryTime(),
            offer.isActive(),
            Timestamp.from(offer.getCreatedAt()),
            Timestamp.from(offer.getUpdatedAt())
        );
    }

    // === READ ALL BY USER ===
    public List<ProductOffer> findByUserId(UUID userId) {
        String sql = "SELECT * FROM product_offer WHERE user_id = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, ROW_MAPPER, userId);
    }

    // === READ ONE ===
    public ProductOffer findById(UUID id) {
        String sql = "SELECT * FROM product_offer WHERE id = ?";
        return jdbcTemplate.queryForObject(sql, ROW_MAPPER, id);
    }

    // === UPDATE ===
    public void update(ProductOffer offer) {
        String sql = """
            UPDATE product_offer SET
                title = ?, description = ?, brand = ?, category = ?,
                original_price = ?, offer_price = ?, discount_percentage = ?,
                stock_quantity = ?, delivery_time = ?, is_active = ?,
                updated_at = ?
            WHERE id = ?
            """;

        jdbcTemplate.update(sql,
            offer.getTitle(),
            offer.getDescription(),
            offer.getBrand(),
            offer.getCategory(),
            offer.getOriginalPrice(),
            offer.getOfferPrice(),
            offer.getDiscountPercentage(),
            offer.getStockQuantity(),
            offer.getDeliveryTime(),
            offer.isActive(),
            Timestamp.from(Instant.now()),
            offer.getId()
        );
    }

    // === DELETE ===
    public void delete(UUID id) {
        jdbcTemplate.update("DELETE FROM product_offer WHERE id = ?", id);
    }

    public List<ProductOffer> findAll() {
        String sql = "SELECT * FROM product_offer ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, ROW_MAPPER);
    }

    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM product_offer");
    }
}