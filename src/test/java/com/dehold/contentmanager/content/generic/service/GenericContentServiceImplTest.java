package com.dehold.contentmanager.content.generic.service;

import com.dehold.contentmanager.content.generic.model.ContentFieldValue;
import com.dehold.contentmanager.content.generic.model.GenericContentModel;
import com.dehold.contentmanager.content.generic.model.ValueType;
import com.dehold.contentmanager.content.generic.repository.GenericModelRepository;
import com.dehold.contentmanager.exception.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GenericContentServiceImplTest {

    @Mock
    private GenericModelRepository repository;

    @InjectMocks
    private GenericContentServiceImpl service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void create_shouldCreateAndReturnContent() {
        GenericContentModel content = createSampleContent();

        doNothing().when(repository).save(content);

        GenericContentModel createdContent = service.create(content);

        assertNotNull(createdContent);
        assertEquals(content.getId(), createdContent.getId());
        assertEquals(content.getUserId(), createdContent.getUserId());
        assertEquals(content.getType(), createdContent.getType());
        verify(repository).save(content);
    }

    @Test
    void getById_shouldReturnContentIfExists() {
        UUID contentId = UUID.randomUUID();
        GenericContentModel content = createSampleContent();

        when(repository.findById(contentId)).thenReturn(Optional.of(content));

        GenericContentModel foundContent = service.getById(contentId);

        assertNotNull(foundContent);
        assertEquals(content.getId(), foundContent.getId());
        assertEquals(content.getType(), foundContent.getType());
        verify(repository).findById(contentId);
    }

    @Test
    void getById_shouldThrowEntityNotFoundExceptionIfNotExists() {
        UUID contentId = UUID.randomUUID();

        when(repository.findById(contentId)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            service.getById(contentId);
        });

        assertTrue(exception.getMessage().contains("GenericContent"));
        assertTrue(exception.getMessage().contains(contentId.toString()));
        verify(repository).findById(contentId);
    }

    @Test
    void update_shouldUpdateAndReturnUpdatedContent() {
        UUID contentId = UUID.randomUUID();
        GenericContentModel existingContent = createSampleContent();
        existingContent.setId(contentId);

        Map<String, ContentFieldValue> updatedFields = new HashMap<>();
        updatedFields.put("title", new ContentFieldValue("title", ValueType.STRING, "Updated Title"));

        GenericContentModel updateRequest = new GenericContentModel(
                contentId,
                existingContent.getUserId(),
                existingContent.getType(),
                updatedFields,
                existingContent.getCreatedAt(),
                Instant.now(),
                existingContent.getParentId()
        );

        when(repository.findById(contentId)).thenReturn(Optional.of(existingContent));
        doNothing().when(repository).save(updateRequest);

        GenericContentModel updatedContent = service.update(updateRequest);

        assertNotNull(updatedContent);
        assertEquals(contentId, updatedContent.getId());
        assertEquals("Updated Title", updatedContent.getFieldNameToValue().get("title").getValue());
        verify(repository).findById(contentId);
        verify(repository).save(updateRequest);
    }

    @Test
    void update_shouldThrowEntityNotFoundExceptionIfNotExists() {
        UUID contentId = UUID.randomUUID();
        GenericContentModel updateRequest = createSampleContent();
        updateRequest.setId(contentId);

        when(repository.findById(contentId)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            service.update(updateRequest);
        });

        assertTrue(exception.getMessage().contains("GenericContent"));
        assertTrue(exception.getMessage().contains(contentId.toString()));
        verify(repository).findById(contentId);
        verify(repository, never()).save(any());
    }

    @Test
    void deleteById_shouldDeleteContentIfExists() {
        UUID contentId = UUID.randomUUID();

        when(repository.existsById(contentId)).thenReturn(true);
        doNothing().when(repository).deleteById(contentId);

        service.deleteById(contentId);

        verify(repository).existsById(contentId);
        verify(repository).deleteById(contentId);
    }

    @Test
    void deleteById_shouldThrowEntityNotFoundExceptionIfNotExists() {
        UUID contentId = UUID.randomUUID();

        when(repository.existsById(contentId)).thenReturn(false);

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            service.deleteById(contentId);
        });

        assertTrue(exception.getMessage().contains("GenericContent"));
        assertTrue(exception.getMessage().contains(contentId.toString()));
        verify(repository).existsById(contentId);
        verify(repository, never()).deleteById(any());
    }

    @Test
    void existsById_shouldReturnTrueIfExists() {
        UUID contentId = UUID.randomUUID();

        when(repository.existsById(contentId)).thenReturn(true);

        boolean exists = service.existsById(contentId);

        assertTrue(exists);
        verify(repository).existsById(contentId);
    }

    @Test
    void existsById_shouldReturnFalseIfNotExists() {
        UUID contentId = UUID.randomUUID();

        when(repository.existsById(contentId)).thenReturn(false);

        boolean exists = service.existsById(contentId);

        assertFalse(exists);
        verify(repository).existsById(contentId);
    }

    private GenericContentModel createSampleContent() {
        Map<String, ContentFieldValue> fields = new HashMap<>();
        fields.put("title", new ContentFieldValue("title", ValueType.STRING, "Sample Title"));
        fields.put("viewCount", new ContentFieldValue("viewCount", ValueType.INTEGER, 100));
        fields.put("rating", new ContentFieldValue("rating", ValueType.DECIMAL, 4.5));
        fields.put("published", new ContentFieldValue("published", ValueType.BOOLEAN, true));

        return new GenericContentModel(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "article",
                fields,
                Instant.now(),
                Instant.now(),
                null
        );
    }
}