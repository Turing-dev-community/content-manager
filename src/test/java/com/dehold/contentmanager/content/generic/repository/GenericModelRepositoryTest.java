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
                "INSERT INTO \"user\" (id, alias, email, created_at, updated_at) VALUES (?, ?, ?, ?, ?)",
                userId,
                "User Alias",
                "user-" + userId + "@example.com",
                Instant.now(),
                Instant.now()
        );
    }

    @Test
    void givenGenericContentDoesNotExist_whenSave_thenInsertsNewContent() {
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
        assertTrue(fieldsJson.contains("views"));
        assertTrue(fieldsJson.contains("42"));
        assertTrue(fieldsJson.contains("published"));
        assertTrue(fieldsJson.contains("true"));
    }

    @Test
    void givenGenericContentExists_whenSave_thenUpdatesExistingContent() throws InterruptedException {
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
}