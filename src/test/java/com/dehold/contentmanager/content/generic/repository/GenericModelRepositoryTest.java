package com.dehold.contentmanager.content.generic.repository;

import com.dehold.contentmanager.content.generic.model.ContentFieldValue;
import com.dehold.contentmanager.content.generic.model.GenericContentModel;
import com.dehold.contentmanager.content.generic.model.ValueType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class GenericModelRepositoryTest {

    @Autowired
    GenericModelRepository cut;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanup() {
        jdbcTemplate.update("DELETE FROM generic_content");
        jdbcTemplate.update("DELETE FROM \"user\"");
    }

    private void insertUser(UUID userId) {
        jdbcTemplate.update(
                "INSERT INTO \"user\" (id, alias, email, username, password, enabled, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                userId,
                "User Alias",
                "user-" + userId + "@example.com",
                "TestUser",
                "TestPassword",
                true,
                Instant.now(),
                Instant.now()
        );
    }

    @Test
    void givenGenericContentDoesNotExist_whenSave_thenInsert() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        insertUser(userId);
        Map<String, ContentFieldValue> fields = new HashMap<>();
        fields.put("title", new ContentFieldValue("title", ValueType.STRING, "Hello"));
        fields.put("views", new ContentFieldValue("views", ValueType.INTEGER, 42));
        fields.put("published", new ContentFieldValue("published", ValueType.BOOLEAN, true));

        GenericContentModel model = new GenericContentModel(
                id,
                userId,
                "blogpost",
                fields,
                Instant.now(),
                Instant.now(),
                null
        );

        cut.save(model);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM generic_content WHERE id = ?",
                Integer.class,
                id
        );
        assertNotNull(count);
        assertEquals(1, count);

        String actualUserId = jdbcTemplate.queryForObject(
                "SELECT user_id FROM generic_content WHERE id = ?",
                String.class,
                id
        );
        assertEquals(actualUserId, userId.toString());

        String type = jdbcTemplate.queryForObject(
                "SELECT type FROM generic_content WHERE id = ?",
                String.class,
                id
        );
        assertEquals("blogpost", type);

        String fieldsJson = jdbcTemplate.queryForObject(
                "SELECT fields FROM generic_content WHERE id = ?",
                String.class,
                id
        );
        assertNotNull(fieldsJson);
        assertTrue(fieldsJson.contains("title"));
        assertTrue(fieldsJson.contains("Hello"));
        assertTrue(fieldsJson.contains("STRING"));
        assertTrue(fieldsJson.contains("views"));
        assertTrue(fieldsJson.contains("INTEGER"));
        assertTrue(fieldsJson.contains("42"));
        assertTrue(fieldsJson.contains("published"));
        assertTrue(fieldsJson.contains("true"));
        assertTrue(fieldsJson.contains("BOOLEAN"));

        String created_at = jdbcTemplate.queryForObject(
                "SELECT created_at FROM generic_content WHERE id = ?",
                String.class,
                id
        );
        assertNotNull(created_at);

        String updated_at = jdbcTemplate.queryForObject(
                "SELECT updated_at FROM generic_content WHERE id = ?",
                String.class,
                id
        );
        assertNotNull(updated_at);
    }

    @Test
    void givenGenericContentExists_whenSave_thenUpdate() throws InterruptedException {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        insertUser(userId);
        Map<String, ContentFieldValue> fields = new HashMap<>();
        fields.put("title", new ContentFieldValue("title", ValueType.STRING, "Initial"));
        GenericContentModel model = new GenericContentModel(
                id,
                userId,
                "blogpost",
                fields,
                Instant.now(),
                Instant.now(),
                null
        );
        cut.save(model);

        String initialFieldsJson = jdbcTemplate.queryForObject(
                "SELECT fields FROM generic_content WHERE id = ?",
                String.class,
                id
        );
        assertNotNull(initialFieldsJson);
        assertTrue(initialFieldsJson.contains("Initial"));

        Map<String, ContentFieldValue> updatedFields = new HashMap<>();
        updatedFields.put("title", new ContentFieldValue("title", ValueType.STRING, "Updated"));
        updatedFields.put("rating", new ContentFieldValue("rating", ValueType.DECIMAL, 4.5));

        GenericContentModel updated = new GenericContentModel(
                id,
                userId,
                "blogpost",
                updatedFields,
                model.getCreatedAt(),
                null,
                null
        );

        Thread.sleep(5);
        cut.save(updated);

        String fieldsJson = jdbcTemplate.queryForObject(
                "SELECT fields FROM generic_content WHERE id = ?",
                String.class,
                id
        );
        assertNotNull(fieldsJson);
        assertTrue(fieldsJson.contains("Updated"));
        assertTrue(fieldsJson.contains("rating"));

        Instant updatedAt = jdbcTemplate.queryForObject(
                "SELECT updated_at FROM generic_content WHERE id = ?",
                Instant.class,
                id
        );
        assertNotNull(updatedAt);
    }

    @Test
    void givenGenericContentId_whenExistsById_thenReturnsFalseBeforeAndTrueAfterSave() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        insertUser(userId);
        Map<String, ContentFieldValue> fields = new HashMap<>();
        fields.put("title", new ContentFieldValue("title", ValueType.STRING, "Title"));

        GenericContentModel model = new GenericContentModel(
                id,
                userId,
                "blogpost",
                fields,
                Instant.now(),
                Instant.now(),
                null
        );

        assertFalse(cut.existsById(id));
        cut.save(model);
        assertTrue(cut.existsById(id));
        assertFalse(cut.existsById(UUID.randomUUID()));
    }

    @Test
    void givenGenericContentExists_whenDeleteById_thenDeleted() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        insertUser(userId);
        Map<String, ContentFieldValue> fields = new HashMap<>();
        fields.put("title", new ContentFieldValue("title", ValueType.STRING, "ToDelete"));

        GenericContentModel model = new GenericContentModel(
                id,
                userId,
                "blogpost",
                fields,
                Instant.now(),
                Instant.now(),
                null
        );
        cut.save(model);

        assertTrue(cut.existsById(id));

        cut.deleteById(id);

        assertFalse(cut.existsById(id));
    }

    @Test
    void givenGenericContentExists_whenFindById_thenReturnsModelWithMappedFields() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        insertUser(userId);
        Map<String, ContentFieldValue> fields = new HashMap<>();
        fields.put("title", new ContentFieldValue("title", ValueType.STRING, "FindMe"));
        fields.put("views", new ContentFieldValue("views", ValueType.INTEGER, 7));
        fields.put("flag", new ContentFieldValue("flag", ValueType.BOOLEAN, true));
        GenericContentModel model = new GenericContentModel(
                id, userId, "blogpost", fields, Instant.now(), Instant.now(), null
        );
        cut.save(model);

        var foundOpt = cut.findById(id);
        assertTrue(foundOpt.isPresent());
        GenericContentModel found = foundOpt.get();
        assertEquals(id, found.getId());
        assertEquals(userId, found.getUserId());
        assertEquals("blogpost", found.getType());
        assertNotNull(found.getFieldNameToValue());
        assertEquals("FindMe", found.getFieldNameToValue().get("title").asString());
        assertEquals(7, found.getFieldNameToValue().get("views").asInteger());
        assertTrue(found.getFieldNameToValue().get("flag").asBoolean());
    }

    @Test
    void givenGenericContentDoesNotExist_whenFindById_thenReturnsEmpty() {
        var result = cut.findById(UUID.randomUUID());
        assertTrue(result.isEmpty());
    }

}
