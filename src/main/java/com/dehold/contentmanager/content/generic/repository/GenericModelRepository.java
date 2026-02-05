package com.dehold.contentmanager.content.generic.repository;

import com.dehold.contentmanager.content.generic.model.ContentFieldValue;
import com.dehold.contentmanager.content.generic.model.GenericContentModel;
import com.dehold.contentmanager.content.generic.model.ValueType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class GenericModelRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final GenericContentRowMapper ROW_MAPPER = new GenericContentRowMapper();

    public GenericModelRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = new ObjectMapper();
    }

    public void save(GenericContentModel model) {
        if (model.getId() == null) {
            model.setId(UUID.randomUUID());
        }
        Optional<GenericContentModel> existing = findById(model.getId());
        if (existing.isPresent()) {
            update(model);
        } else {
            insert(model);
        }
    }

    public boolean existsById(UUID id) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM generic_content WHERE id = ?",
                Integer.class,
                id
        );
        return count != null && count > 0;
    }

    public void deleteById(UUID id) {
        jdbcTemplate.update("DELETE FROM generic_content WHERE id = ?", id);
    }

    public Optional<GenericContentModel> findById(UUID id) {
        var list = jdbcTemplate.query(
                "SELECT * FROM generic_content WHERE id = ?",
                ROW_MAPPER,
                id
        );
        if (list.isEmpty()) return Optional.empty();
        return Optional.of(list.getFirst());
    }

    public java.util.List<GenericContentModel> findByUserIdAndContentType(UUID userId, String contentType) {
        return jdbcTemplate.query(
                "SELECT * FROM generic_content WHERE user_id = ? AND type = ?",
                ROW_MAPPER,
                userId,
                contentType
        );
    }

    private void insert(GenericContentModel model) {
        if (model.getCreatedAt() == null) model.setCreatedAt(Instant.now());
        if (model.getUpdatedAt() == null) model.setUpdatedAt(model.getCreatedAt());
        String fieldsJson = serializeFields(model.getFieldNameToValue());
        jdbcTemplate.update(
                "INSERT INTO generic_content (id, user_id, type, fields, created_at, updated_at, parent_id) VALUES (?, ?, ?, ?, ?, ?, ?)",
                model.getId(),
                model.getUserId(),
                model.getType(),
                fieldsJson,
                model.getCreatedAt(),
                model.getUpdatedAt(),
                model.getParentId()
        );
    }

    private void update(GenericContentModel model) {
        model.setUpdatedAt(Instant.now());
        String fieldsJson = serializeFields(model.getFieldNameToValue());
        jdbcTemplate.update(
                "UPDATE generic_content SET user_id = ?, type = ?, fields = ?, updated_at = ?, parent_id = ? WHERE id = ?",
                model.getUserId(),
                model.getType(),
                fieldsJson,
                model.getUpdatedAt(),
                model.getParentId(),
                model.getId()
        );
    }

    private String serializeFields(Map<String, ContentFieldValue> fields) {
        try {
            if (fields == null) return null;
            Map<String, Map<String, Object>> envelope = new HashMap<>();
            for (Map.Entry<String, ContentFieldValue> e : fields.entrySet()) {
                ContentFieldValue v = e.getValue();
                Map<String, Object> payload = new HashMap<>();
                payload.put("type", v.getValueType().name().toLowerCase());
                payload.put("value", v.getValue());
                envelope.put(e.getKey(), payload);
            }
            return objectMapper.writeValueAsString(envelope);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize fields", e);
        }
    }

    private Map<String, ContentFieldValue> deserializeFields(String json) {
        try {
            if (json == null || json.isEmpty()) return Map.of();
            String cleanJson = stripRedundantQuotes(json);
            Map<String, Map<String, Object>> envelope = objectMapper.readValue(
                    cleanJson,
                    new TypeReference<Map<String, Map<String, Object>>>() {}
            );
            Map<String, ContentFieldValue> result = new HashMap<>();
            for (Map.Entry<String, Map<String, Object>> e : envelope.entrySet()) {
                String field = e.getKey();
                Map<String, Object> payload = e.getValue();
                String typeStr = String.valueOf(payload.get("type"));
                ValueType vt = ValueType.valueOf(typeStr);
                Object raw = payload.get("value");
                Object coerced = coerce(raw, vt);
                result.put(field, new ContentFieldValue(field, vt, coerced));
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize fields", e);
        }
    }

    private static String stripRedundantQuotes(String json) {
        String clean = json;
        if (json.startsWith("\"") && json.endsWith("\"")) {
            clean = json.substring(1, json.length() - 1).replace("\\\"", "\"");
        }
        return clean;
    }

    private Object coerce(Object raw, ValueType vt) {
        if (raw == null) return null;
        return switch (vt) {
            case STRING -> raw.toString();
            case INTEGER -> (raw instanceof Integer) ? raw : ((Number) raw).intValue();
            case DECIMAL -> (raw instanceof Double) ? raw : ((Number) raw).doubleValue();
            case BOOLEAN -> (raw instanceof Boolean) ? raw : Boolean.valueOf(raw.toString());
        };
    }

    private class GenericContentRowMapper implements RowMapper<GenericContentModel> {
        @Override
        public GenericContentModel mapRow(ResultSet rs, int rowNum) throws SQLException {
            String json = rs.getString("fields");
            Map<String, ContentFieldValue> fields = deserializeFields(json);
            return new GenericContentModel(
                    UUID.fromString(rs.getString("id")),
                    UUID.fromString(rs.getString("user_id")),
                    rs.getString("type"),
                    fields,
                    rs.getTimestamp("created_at").toInstant(),
                    rs.getTimestamp("updated_at").toInstant(),
                    rs.getString("parent_id") == null ? null : UUID.fromString(rs.getString("parent_id"))
            );
        }
    }
}
