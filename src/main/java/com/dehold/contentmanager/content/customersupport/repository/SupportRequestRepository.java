package com.dehold.contentmanager.content.customersupport.repository;

import com.dehold.contentmanager.content.customersupport.model.SupportRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class SupportRequestRepository {
    private final JdbcTemplate jdbcTemplate;

    public SupportRequestRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<SupportRequest> ROW_MAPPER = new RowMapper<>() {
        @Override
        public SupportRequest mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new SupportRequest(
                UUID.fromString(rs.getString("id")),
                UUID.fromString(rs.getString("user_id")),
                rs.getString("text"),
                UUID.fromString(rs.getString("support_response")),
                UUID.fromString(rs.getString("customer_id")),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()
            );
        }
    };

    public void create(SupportRequest request) {
        String sql = "INSERT INTO customer_request (id, user_id, text, support_response, customer_id, created_at, " +
                "updated_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, request.getId(), request.getUserId(), request.getText(), request.getSupportResponse(),
                request.getCustomerId(), request.getCreatedAt(), request.getUpdatedAt());
    }

    public void update(SupportRequest request) {
        String sql = "UPDATE customer_request SET text = ?, support_response = ?, customer_id = ?, created_at = " +
                "?, updated_at = ? WHERE id = ?";
        jdbcTemplate.update(sql, request.getText(), request.getSupportResponse(), request.getCustomerId(), request.getCreatedAt(), request.getUpdatedAt(), request.getId());
    }

    public List<SupportRequest> findAll() {
        String sql = "SELECT * FROM customer_request";
        return jdbcTemplate.query(sql, ROW_MAPPER);
    }

    public Optional<SupportRequest> getById(UUID id) {
        String sql = "SELECT * FROM customer_request WHERE id = ?";
        return jdbcTemplate.query(sql, ROW_MAPPER, id).stream().findFirst();
    }

    public void deleteById(UUID id) {
        String sql = "DELETE FROM customer_request WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }
}
