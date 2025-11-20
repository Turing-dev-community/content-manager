package com.dehold.contentmanager.ratelimiter.repository;

import com.dehold.contentmanager.ratelimiter.config.RateLimitConfig;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class RateLimitConfigRepository {

    private final JdbcTemplate jdbc;

    public RateLimitConfigRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<RateLimitConfig> ROW_MAPPER = new RowMapper<>() {
        @Override
        public RateLimitConfig mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new RateLimitConfig(
                    UUID.fromString(rs.getString("id")),
                    rs.getString("path_pattern"),
                    rs.getLong("capacity"),
                    rs.getLong("refill_tokens"),
                    rs.getLong("refill_interval_millis"),
                    rs.getTimestamp("created_at").toInstant(),
                    rs.getTimestamp("updated_at").toInstant()
            );
        }
    };

    public List<RateLimitConfig> findAll() {
        return jdbc.query("SELECT * FROM rate_limit_config", ROW_MAPPER);
    }

    public Optional<RateLimitConfig> findByPathPattern(String pathPattern) {
        String sql = "SELECT * FROM rate_limit_config WHERE path_pattern = ?";
        return jdbc.query(sql, ROW_MAPPER, pathPattern).stream().findFirst();
    }

    public Optional<RateLimitConfig> findById(UUID id) {
        String sql = "SELECT * FROM rate_limit_config WHERE id = ?";
        return jdbc.query(sql, ROW_MAPPER, id.toString()).stream().findFirst();
    }

    public void insert(RateLimitConfig cfg) {
        String sql = "INSERT INTO rate_limit_config (id, path_pattern, capacity, refill_tokens, refill_interval_millis, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
        jdbc.update(sql, cfg.getId().toString(), cfg.getPathPattern(), cfg.getCapacity(), cfg.getRefillTokens(), cfg.getRefillIntervalMillis(), cfg.getCreatedAt(), cfg.getUpdatedAt());
    }

    public void update(RateLimitConfig cfg) {
        String sql = "UPDATE rate_limit_config SET capacity = ?, refill_tokens = ?, refill_interval_millis = ?, updated_at = ? WHERE id = ?";
        jdbc.update(sql, cfg.getCapacity(), cfg.getRefillTokens(), cfg.getRefillIntervalMillis(), cfg.getUpdatedAt(), cfg.getId().toString());
    }

    public void delete(UUID id) {
        jdbc.update("DELETE FROM rate_limit_config WHERE id = ?", id.toString());
    }
}
