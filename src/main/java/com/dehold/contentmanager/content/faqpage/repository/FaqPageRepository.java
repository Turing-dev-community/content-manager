package com.dehold.contentmanager.content.faqpage.repository;

import com.dehold.contentmanager.content.faqpage.model.FaqItem;
import com.dehold.contentmanager.content.faqpage.model.FaqPage;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class FaqPageRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FaqPageRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void createFaqPage(FaqPage page) {
        if (page.getId() == null) page.setId(UUID.randomUUID());
        if (page.getCreatedAt() == null) page.setCreatedAt(Instant.now());
        if (page.getUpdatedAt() == null) page.setUpdatedAt(page.getCreatedAt());
        try {
            String itemsJson = objectMapper.writeValueAsString(page.getFaqItems());
            jdbcTemplate.update(
                    "INSERT INTO faq_page (id, user_id, title, introduction, faq_items, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    page.getId(),
                    page.getUserId(),
                    page.getTitle(),
                    page.getIntroduction(),
                    itemsJson,
                    page.getCreatedAt(),
                    page.getUpdatedAt()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize faq items", e);
        }
    }

    public Optional<FaqPage> getFaqPage(UUID id) {
        List<FaqPage> list = jdbcTemplate.query(
                "SELECT * FROM faq_page WHERE id = ?",
                new FaqPageRowMapper(objectMapper),
                id
        );
        if (list.isEmpty()) return Optional.empty();
        return Optional.of(list.get(0));
    }

    public List<FaqPage> getFaqPagesByUserId(UUID userId) {
        return jdbcTemplate.query(
                "SELECT * FROM faq_page WHERE user_id = ?",
                new FaqPageRowMapper(objectMapper),
                userId
        );
    }

    private static class FaqPageRowMapper implements RowMapper<FaqPage> {
        private final ObjectMapper objectMapper;

        public FaqPageRowMapper(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public FaqPage mapRow(ResultSet rs, int rowNum) throws SQLException {
            try {
                String itemsJson = rs.getString("faq_items");
                List<FaqItem> items = itemsJson == null || itemsJson.isEmpty() ? List.of() : objectMapper.readValue(itemsJson, new TypeReference<List<FaqItem>>(){});
                return new FaqPage(
                        UUID.fromString(rs.getString("id")),
                        rs.getString("title"),
                        rs.getString("introduction"),
                        items,
                        rs.getTimestamp("created_at").toInstant(),
                        rs.getTimestamp("updated_at").toInstant(),
                        UUID.fromString(rs.getString("user_id"))
                );
            } catch (Exception e) {
                throw new RuntimeException("Failed to deserialize faq items", e);
            }
        }
    }
}
