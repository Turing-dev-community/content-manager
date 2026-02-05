package com.dehold.contentmanager.validation.repository;

import com.dehold.contentmanager.validation.model.ValidationError;
import com.dehold.contentmanager.validation.model.ValidationResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Repository
public class ValidationResultRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public ValidationResultRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public void create(ValidationResult validationResult) {
        jdbcTemplate.update(
                "INSERT INTO validation_result (id, user_id, content_id, content_type, is_valid, errors, created_at, run_id) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                validationResult.getId(),
                validationResult.getUserId(),
                validationResult.getContentId(),
                validationResult.getContentType(),
                validationResult.isValid(),
                serializeErrors(validationResult.getErrors()),
                validationResult.getCreatedAt(),
                UUID.randomUUID()
        );
    }

    public List<ValidationResult> findAll() {
        return jdbcTemplate.query("SELECT * FROM validation_result", this::mapRowToValidationResult);
    }

    public List<ValidationResult> findByUserId(UUID id) {
        return jdbcTemplate.query("SELECT * FROM validation_result WHERE user_id = ?", this::mapRowToValidationResult,
                id);
    }

    private ValidationResult mapRowToValidationResult(ResultSet rs, int rowNum) throws SQLException {
        return ValidationResult.fromPersistence(
                UUID.fromString(rs.getString("id")),
                UUID.fromString(rs.getString("user_id")),
                rs.getString("content_type"),
                UUID.fromString(rs.getString("content_id")),
                rs.getBoolean("is_valid"),
                deserializeErrors(rs.getString("errors")),
                rs.getTimestamp("created_at").toInstant()
        );
    }

    private String serializeErrors(List<ValidationError> errors) {
        try {
            return objectMapper.writeValueAsString(errors);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize errors", e);
        }
    }

    private List<ValidationError> deserializeErrors(String errorsJson) {
        try {
            if (errorsJson == null || errorsJson.isEmpty()) {
                return Collections.emptyList();
            }
            return objectMapper.readValue(errorsJson, new TypeReference<List<ValidationError>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public void upsert(ValidationResult validationResult, UUID runId) {
        jdbcTemplate.update(
                "MERGE INTO validation_result AS vr " +
                        "USING (VALUES (?, ?, ?, ?, ?, ?, ?, ?)) AS vals(" +
                        "id, user_id, content_id, content_type, is_valid, errors, created_at, run_id" +
                        ") " +
                        "ON vr.run_id = vals.run_id " +
                        "AND vr.user_id = vals.user_id " +
                        "AND vr.content_id = vals.content_id " +
                        "AND vr.content_type = vals.content_type " +
                        "WHEN MATCHED THEN UPDATE SET " +
                        "vr.is_valid = vals.is_valid, " +
                        "vr.errors = vals.errors " +
                        "WHEN NOT MATCHED THEN INSERT (" +
                        "id, user_id, content_id, content_type, is_valid, errors, created_at, run_id" +
                        ") VALUES (" +
                        "vals.id, vals.user_id, vals.content_id, vals.content_type, " +
                        "vals.is_valid, vals.errors, vals.created_at, vals.run_id" +
                        ")",
                validationResult.getId(),
                validationResult.getUserId(),
                validationResult.getContentId(),
                validationResult.getContentType(),
                validationResult.isValid(),
                serializeErrors(validationResult.getErrors()),
                validationResult.getCreatedAt(),
                runId
        );
    }

}
