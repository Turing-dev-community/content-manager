package com.dehold.contentmanager.content.customersupport.repository;

import com.dehold.contentmanager.content.customersupport.model.SupportResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class SupportResponseRepository {
    private final JdbcTemplate jdbcTemplate;

    public SupportResponseRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<SupportResponse> ROW_MAPPER = new RowMapper<>() {
        @Override
        public SupportResponse mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new SupportResponse(
                UUID.fromString(rs.getString("id")),
                UUID.fromString(rs.getString("user_id")),
                rs.getString("text"),
                UUID.fromString(rs.getString("support_request")),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()
            );
        }
    };

    public void create(SupportResponse response) {
        String sql = "INSERT INTO support_response (id, user_id, text, support_request, created_at, updated_at) " +
                "VALUES" +
                " (?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, response.getId(), response.getUserId(), response.getText(),
                response.getSupportRequest(),
                response.getCreatedAt(), response.getUpdatedAt());
    }

    public Optional<SupportResponse> getById(UUID id) {
        String sql = "SELECT * FROM support_response WHERE id = ?";
        return jdbcTemplate.query(sql, ROW_MAPPER, id).stream().findFirst();
    }

    public void update(SupportResponse response) {
        String sql = "UPDATE support_response SET text = ?, support_request = ?, created_at = ?, updated_at = ? WHERE id = ?";
        jdbcTemplate.update(sql, response.getText(), response.getSupportRequest(), response.getCreatedAt(), response.getUpdatedAt(), response.getId());
    }

    public void delete(UUID id) {
        String sql = "DELETE FROM support_response WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    public List<SupportResponse> getSupportResponsesByUserId(UUID userId) {
        String sql = "SELECT sr.* FROM support_response sr " +
        "JOIN customer_request cr ON sr.support_request = cr.id " +
        "WHERE cr.customer_id = ?";
        return jdbcTemplate.query(sql, ROW_MAPPER, userId);
        }
}

