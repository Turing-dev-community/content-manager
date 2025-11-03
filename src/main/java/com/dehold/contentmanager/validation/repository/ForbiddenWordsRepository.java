package com.dehold.contentmanager.validation.repository;

import com.dehold.contentmanager.validation.model.ForbiddenWords;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Repository
public class ForbiddenWordsRepository {

    private final JdbcTemplate jdbcTemplate;

    private final ForbiddenWordsRowMapper FORBIDDEN_WORDS_ROW_MAPPER = new ForbiddenWordsRowMapper();

    public ForbiddenWordsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(ForbiddenWords forbiddenWords) {
        Optional<ForbiddenWords> existing = findById(forbiddenWords.getId());
        if (existing.isPresent()) {
            update(forbiddenWords);
        } else {
            insert(forbiddenWords);
        }
    }

    private void insert(ForbiddenWords forbiddenWords) {
        forbiddenWords.setId(UUID.randomUUID());
        forbiddenWords.setCreatedAt(java.time.Instant.now());
        forbiddenWords.setUpdatedAt(java.time.Instant.now());
        jdbcTemplate.update(
                "INSERT INTO forbidden_words (id, user_id, description, content_type, field_name, words, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                forbiddenWords.getId(),
                forbiddenWords.getUserId(),
                forbiddenWords.getDescription(),
                forbiddenWords.getContentType(),
                forbiddenWords.getFieldName(),
                String.join(",", forbiddenWords.getWords()),
                forbiddenWords.getCreatedAt(),
                forbiddenWords.getUpdatedAt()
        );
    }

    private void update(ForbiddenWords forbiddenWords) {
        forbiddenWords.setUpdatedAt(java.time.Instant.now());
        jdbcTemplate.update(
                "UPDATE forbidden_words SET user_id = ?, description = ?, content_type = ?, field_name = ?, words = ?, updated_at = ? WHERE id = ?",
                forbiddenWords.getUserId(),
                forbiddenWords.getDescription(),
                forbiddenWords.getContentType(),
                forbiddenWords.getFieldName(),
                String.join(",", forbiddenWords.getWords()),
                forbiddenWords.getUpdatedAt(),
                forbiddenWords.getId()
        );
    }


    public Optional<ForbiddenWords> findById(UUID id) {
        String sql = "SELECT * FROM forbidden_words WHERE id = ?";
        return jdbcTemplate.query(sql, FORBIDDEN_WORDS_ROW_MAPPER, id).stream().findFirst();
    }

    public List<ForbiddenWords> findAll() {
        return jdbcTemplate.query("SELECT * FROM forbidden_words", new ForbiddenWordsRowMapper());
    }

    public void delete(ForbiddenWords forbiddenWords) {
        jdbcTemplate.update(
                "DELETE FROM forbidden_words WHERE id = ?",
                forbiddenWords.getId()
        );
    }

    public ForbiddenWords findByUserIdAndContentType(UUID userId, String contentType) {
        String sql = "SELECT * FROM forbidden_words WHERE user_id = ? AND content_type = ?";
        return jdbcTemplate.query(sql, FORBIDDEN_WORDS_ROW_MAPPER, userId, contentType).stream().findFirst()
                .orElse(null);
    }

    private static class ForbiddenWordsRowMapper implements RowMapper<ForbiddenWords> {
        @Override
        public ForbiddenWords mapRow(ResultSet rs, int rowNum) throws SQLException {
            String wordsAsString = rs.getString("words");
            LinkedHashSet<String> words = new LinkedHashSet<String>(Arrays.asList(wordsAsString.split(",")));
            ForbiddenWords forbiddenWords = new ForbiddenWords(
                    UUID.fromString(rs.getString("id")),
                    UUID.fromString(rs.getString("user_id")),
                    rs.getString("description"),
                    rs.getString("content_type"),
                    rs.getString("field_name"),
                    words
            );
            forbiddenWords.setCreatedAt(rs.getTimestamp("created_at").toInstant());
            forbiddenWords.setUpdatedAt(rs.getTimestamp("updated_at").toInstant());
            return forbiddenWords;
        }
    }
}
