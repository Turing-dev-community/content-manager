package com.dehold.contentmanager.user.repository;

import com.dehold.contentmanager.user.model.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<User> USER_ROW_MAPPER = new RowMapper<>() {
        @Override
        public User mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new User(
                UUID.fromString(rs.getString("id")),
                rs.getString("alias"),
                rs.getString("email"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant(),
                    rs.getString("username"),
                    rs.getString("password"),
                    rs.getBoolean("enabled")
            );
        }
    };

    public void createUser(User user) {
        String sql = "INSERT INTO \"user\" (id, alias, email, username, password, enabled, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, user.getId(), user.getAlias(), user.getEmail(), user.getUsername(), user.getPassword(), user.isEnabled(), user.getCreatedAt(), user.getUpdatedAt());
    }

    public Optional<User> getUserById(UUID id) {
        String sql = "SELECT * FROM \"user\" WHERE id = ?";
        return jdbcTemplate.query(sql, USER_ROW_MAPPER, id).stream().findFirst();
    }

    public Optional<User> getUserByEmail(String email) {
        String sql = "SELECT * FROM \"user\" WHERE email = ?";
        return jdbcTemplate.query(sql, USER_ROW_MAPPER, email).stream().findFirst();
    }

    public void updateUser(User user) {
        String sql = "UPDATE \"user\" " +
                "SET alias = ?, email = ?, username = ?, password = ?, enabled = ?, updated_at = ? " +
                "WHERE id = ?";

        jdbcTemplate.update(sql,
                user.getAlias(),
                user.getEmail(),
                user.getUsername(),
                user.getPassword(),
                user.isEnabled(),
                user.getUpdatedAt(),
                user.getId()
        );
    }

    public void deleteUser(UUID id) {
        String sql = "DELETE FROM \"user\" WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    public void insertAuthority(String username, String role) {
        String sql = "INSERT INTO authorities (username, authority) VALUES (?, ?)";
        jdbcTemplate.update(sql, username, role);
    }

    public void updateAuthorityUsername(String oldUsername, String newUsername) {
        String authoritiesSql = "UPDATE authorities SET username = ? WHERE username = ?";
        jdbcTemplate.update(authoritiesSql, newUsername, oldUsername);
    }

    public void deleteSecurityAuthorities(String username) {
        String sql = "DELETE FROM authorities WHERE username = ?";
        jdbcTemplate.update(sql, username);
    }

}
