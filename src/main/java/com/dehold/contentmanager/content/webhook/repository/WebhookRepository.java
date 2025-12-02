package com.dehold.contentmanager.content.webhook.repository;

import com.dehold.contentmanager.content.webhook.model.Webhook;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class WebhookRepository {

    private final JdbcTemplate jdbcTemplate;

    public WebhookRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<Webhook> ROW_MAPPER = (rs, rowNum) -> new Webhook(
        UUID.fromString(rs.getString("id")),
        UUID.fromString(rs.getString("user_id")),
        rs.getString("url"),
        rs.getTimestamp("created_at").toInstant(),
        rs.getTimestamp("updated_at").toInstant()
    );

    public void create(Webhook webhook) {
        String sql = """
            INSERT INTO webhook (id, user_id, url, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?)
            """;
        jdbcTemplate.update(sql,
            webhook.getId(),
            webhook.getUserId(),
            webhook.getUrl(),
            Timestamp.from(webhook.getCreatedAt()),
            Timestamp.from(webhook.getUpdatedAt())
        );
    }

    public List<Webhook> findByUserId(UUID userId) {
        String sql = "SELECT * FROM webhook WHERE user_id = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, ROW_MAPPER, userId);
    }

    public Webhook findById(UUID id) {
        String sql = "SELECT * FROM webhook WHERE id = ?";
        return jdbcTemplate.queryForObject(sql, ROW_MAPPER, id);
    }

    public void update(Webhook webhook) {
        String sql = """
            UPDATE webhook SET url = ?, updated_at = ? WHERE id = ?
            """;
        jdbcTemplate.update(sql,
            webhook.getUrl(),
            Timestamp.from(Instant.now()),
            webhook.getId()
        );
    }

    public void delete(UUID id) {
        jdbcTemplate.update("DELETE FROM webhook WHERE id = ?", id);
    }

    public List<Webhook> findAll() {
        String sql = "SELECT * FROM webhook ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, ROW_MAPPER);
    }
}