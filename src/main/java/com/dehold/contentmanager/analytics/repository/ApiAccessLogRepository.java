package com.dehold.contentmanager.analytics.repository;

import com.dehold.contentmanager.analytics.model.ApiAccessLog;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@Repository
public class ApiAccessLogRepository {

    private final JdbcTemplate jdbcTemplate;

    public ApiAccessLogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<ApiAccessLog> API_ACCESS_LOG_ROW_MAPPER = new RowMapper<>() {
        @Override
        public ApiAccessLog mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new ApiAccessLog(
                    UUID.fromString(rs.getString("id")),
                    rs.getString("url"),
                    rs.getTimestamp("timestamp").toInstant()
            );
        }
    };

    public void save(ApiAccessLog apiAccessLog) {
        String sql = "INSERT INTO api_access_log (id, url, timestamp) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, apiAccessLog.getId(), apiAccessLog.getUrl(), apiAccessLog.getTimestamp());
    }

    public List<ApiAccessLog> findAll() {
        return jdbcTemplate.query("SELECT * FROM api_access_log", API_ACCESS_LOG_ROW_MAPPER);
    }
}
