package com.dehold.contentmanager.validation.repository;

import com.dehold.contentmanager.validation.model.ValidationPipelineModel;
import com.dehold.contentmanager.validation.model.ValidationStepModel;
import com.dehold.contentmanager.validation.model.ValidationStepType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;

@Repository
public class ValidationPipelineRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final ValidationPipelineRowMapper VALIDATION_PIPELINE_ROW_MAPPER = new ValidationPipelineRowMapper();
    private final ValidationStepRowMapper VALIDATION_STEP_ROW_MAPPER = new ValidationStepRowMapper();

    public ValidationPipelineRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = new ObjectMapper();
    }

    public ValidationPipelineModel save(ValidationPipelineModel pipeline) {
        Optional<ValidationPipelineModel> existing = findById(pipeline.getId());
        if (existing.isPresent()) {
            update(pipeline);
        } else {
            insert(pipeline);
        }
    }

    private void insert(ValidationPipelineModel pipeline) {
        if (pipeline.getId() == null) {
            pipeline.setId(UUID.randomUUID());
        }
        if (pipeline.getCreatedAt() == null) {
            pipeline.setCreatedAt(Instant.now());
        }

        jdbcTemplate.update(
                "INSERT INTO validation_pipeline (id, user_id, description, content_type, created_at) VALUES (?, ?, ?, ?, ?)",
                pipeline.getId(),
                pipeline.getUserId(),
                pipeline.getDescription(),
                pipeline.getContentType(),
                pipeline.getCreatedAt()
        );

        if (pipeline.getSteps() != null) {
            for (ValidationStepModel step : pipeline.getSteps()) {
                insertValidationStep(step, pipeline.getId());
            }
        }
    }

    private void update(ValidationPipelineModel pipeline) {
        jdbcTemplate.update(
                "UPDATE validation_pipeline SET user_id = ?, description = ?, content_type = ? WHERE id = ?",
                pipeline.getUserId(),
                pipeline.getDescription(),
                pipeline.getContentType(),
                pipeline.getId()
        );

        jdbcTemplate.update("DELETE FROM validation_step WHERE pipeline_id = ?", pipeline.getId());
        if (pipeline.getSteps() != null) {
            for (ValidationStepModel step : pipeline.getSteps()) {
                insertValidationStep(step, pipeline.getId());
            }
        }
    }

    private void insertValidationStep(ValidationStepModel step, UUID pipelineId) {
        try {
            String parametersJson = objectMapper.writeValueAsString(step.getParameters());
            jdbcTemplate.update(
                    "INSERT INTO validation_step (id, pipeline_id, step_type, field_name, parameters, is_enabled) VALUES (?, ?, ?, ?, ?, ?)",
                    step.getId() != null ? step.getId() : UUID.randomUUID(),
                    pipelineId,
                    step.getStepType().name(),
                    step.getFieldName(),
                    parametersJson,
                    step.isEnabled()
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize validation step parameters", e);
        }
    }

    public List<ValidationPipelineModel> findAll() {
        List<ValidationPipelineModel> pipelines = jdbcTemplate.query(
                "SELECT * FROM validation_pipeline ORDER BY created_at",
                VALIDATION_PIPELINE_ROW_MAPPER
        );

        for (ValidationPipelineModel pipeline : pipelines) {
            List<ValidationStepModel> steps = loadStepsForPipeline(pipeline.getId());
            pipeline.setSteps(steps);
        }

        return pipelines;
    }

    public Optional<ValidationPipelineModel> findByUserIdAndContentType(UUID userId, String contentType) {
        List<ValidationPipelineModel> pipelines = jdbcTemplate.query(
                "SELECT * FROM validation_pipeline WHERE user_id = ? AND content_type = ?",
                VALIDATION_PIPELINE_ROW_MAPPER,
                userId,
                contentType
        );

        if (pipelines.isEmpty()) {
            return Optional.empty();
        }

        ValidationPipelineModel pipeline = pipelines.getFirst();
        List<ValidationStepModel> steps = loadStepsForPipeline(pipeline.getId());
        pipeline.setSteps(steps);

        return Optional.of(pipeline);
    }

    public void deleteById(UUID id) {
        jdbcTemplate.update("DELETE FROM validation_step WHERE pipeline_id = ?", id);
        jdbcTemplate.update("DELETE FROM validation_pipeline WHERE id = ?", id);
    }

    private Optional<ValidationPipelineModel> findById(UUID id) {
        List<ValidationPipelineModel> pipelines = jdbcTemplate.query(
                "SELECT * FROM validation_pipeline WHERE id = ?",
                VALIDATION_PIPELINE_ROW_MAPPER,
                id
        );

        if (pipelines.isEmpty()) {
            return Optional.empty();
        }

        ValidationPipelineModel pipeline = pipelines.get(0);
        List<ValidationStepModel> steps = loadStepsForPipeline(pipeline.getId());
        pipeline.setSteps(steps);

        return Optional.of(pipeline);
    }

    private List<ValidationStepModel> loadStepsForPipeline(UUID pipelineId) {
        return jdbcTemplate.query(
                "SELECT * FROM validation_step WHERE pipeline_id = ? ORDER BY id",
                VALIDATION_STEP_ROW_MAPPER,
                pipelineId
        );
    }

    private class ValidationPipelineRowMapper implements RowMapper<ValidationPipelineModel> {
        @Override
        public ValidationPipelineModel mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new ValidationPipelineModel(
                    UUID.fromString(rs.getString("id")),
                    UUID.fromString(rs.getString("user_id")),
                    rs.getString("description"),
                    rs.getString("content_type"),
                    new ArrayList<>(), // Steps will be loaded separately
                    rs.getTimestamp("created_at").toInstant()
            );
        }
    }

    private class ValidationStepRowMapper implements RowMapper<ValidationStepModel> {
        @Override
        public ValidationStepModel mapRow(ResultSet rs, int rowNum) throws SQLException {
            try {
                String parametersJson = rs.getString("parameters");
                Map<String, String> parameters = new HashMap<>();

                if (parametersJson != null && !parametersJson.trim().isEmpty()) {
                    String cleanJson = stripRedundantQuotes(parametersJson);
                    parameters = objectMapper.readValue(cleanJson, new TypeReference<Map<String, String>>() {
                    });
                }

                return new ValidationStepModel(
                        UUID.fromString(rs.getString("id")),
                        UUID.fromString(rs.getString("pipeline_id")),
                        ValidationStepType.valueOf(rs.getString("step_type")),
                        rs.getString("field_name"),
                        parameters,
                        rs.getBoolean("is_enabled")
                );
            } catch (JsonProcessingException e) {
                throw new SQLException("Failed to deserialize validation step parameters: " + e.getMessage(), e);
            }
        }

        private static String stripRedundantQuotes(String parametersJson) {
            String cleanJson = parametersJson;
            if (parametersJson.startsWith("\"") && parametersJson.endsWith("\"")) {
                cleanJson = parametersJson.substring(1, parametersJson.length() - 1)
                        .replace("\\\"", "\"");
            }
            return cleanJson;
        }
    }
}
