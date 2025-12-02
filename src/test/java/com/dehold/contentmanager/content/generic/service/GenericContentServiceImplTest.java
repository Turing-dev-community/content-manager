package com.dehold.contentmanager.content.generic.service;

import com.dehold.contentmanager.content.generic.model.ContentFieldValue;
import com.dehold.contentmanager.content.generic.model.GenericContentModel;
import com.dehold.contentmanager.content.generic.model.ValueType;
import com.dehold.contentmanager.content.generic.repository.GenericModelRepository;
import com.dehold.contentmanager.exception.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
class GenericContentServiceImplTest {

    @MockitoBean
    private GenericModelRepository repository;

    @Autowired
    private GenericContentServiceImpl service;

    @Autowired
    private CacheManager cacheManager;

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

        assertThrows(EntityNotFoundException.class, () -> {
            service.getById(contentId);
        });
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

        assertThrows(EntityNotFoundException.class, () -> {
            service.update(updateRequest);
        });

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

        assertThrows(EntityNotFoundException.class, () -> {
            service.deleteById(contentId);
        });
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
    
    @Test
    void getById_shouldReturnContentAndCacheResult() {
        clearCaches();
        UUID id = UUID.randomUUID();
        GenericContentModel content = createSampleContent();
        content.setId(id);

        when(repository.findById(id)).thenReturn(Optional.of(content));

        GenericContentModel first = service.getById(id);
        GenericContentModel second = service.getById(id);

        assertSame(first, second, "Should return same instance from cache");
        verify(repository, times(1)).findById(id);
    }

    @Test
    void findByUserIdAndContentType_shouldCacheResultOnSecondCall() {
        clearCaches();
        UUID userId = UUID.randomUUID();
        String contentType = "article";
        List<GenericContentModel> list = List.of(createSampleContent());

        when(repository.findByUserIdAndContentType(userId, contentType)).thenReturn(list);

        List<GenericContentModel> first = service.findByUserIdAndContentType(userId, contentType);
        List<GenericContentModel> second = service.findByUserIdAndContentType(userId, contentType);

        assertSame(first, second, "Should return same list instance from cache");
        verify(repository, times(1)).findByUserIdAndContentType(userId, contentType);
    }

    @Test
    void create_shouldEvictBothCaches() {
        clearCaches();
        UUID userId = UUID.randomUUID();
        String contentType = "article";
        GenericContentModel newContent = createSampleContent();
        newContent.setUserId(userId);
        newContent.setType(contentType);

        when(repository.findByUserIdAndContentType(userId, contentType)).thenReturn(List.of());
        service.findByUserIdAndContentType(userId, contentType);
        verify(repository, times(1)).findByUserIdAndContentType(userId, contentType);

        doNothing().when(repository).save(any());
        service.create(newContent);

        service.findByUserIdAndContentType(userId, contentType);
        verify(repository, times(2)).findByUserIdAndContentType(userId, contentType);
    }

    @Test
    void update_shouldEvictBothCaches() {
        clearCaches();

        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String contentType = "article";

        GenericContentModel original = createSampleContent();
        original.setId(id);
        original.setUserId(userId);
        original.setType(contentType);

        GenericContentModel updated = createSampleContent();
        updated.setId(id);
        updated.setUserId(userId);
        updated.setType(contentType);
        updated.getFieldNameToValue().put("title", new ContentFieldValue("title", ValueType.STRING, "Updated Title"));

        when(repository.findById(id))
              .thenReturn(Optional.of(original)) // 1st call (priming getById)
              .thenReturn(Optional.of(original)) // 2nd call (internal read inside update)
              .thenReturn(Optional.of(updated)); // 3rd call (post-eviction getById)
        when(repository.findByUserIdAndContentType(userId, contentType)).thenReturn(List.of(original));

        service.getById(id);                                 
        service.findByUserIdAndContentType(userId, contentType); 

        verify(repository, times(1)).findByUserIdAndContentType(userId, contentType);

        doNothing().when(repository).save(any());
        service.update(updated); // This is the 2nd findById call

        when(repository.findByUserIdAndContentType(userId, contentType)).thenReturn(List.of(updated));

        service.getById(id);                                 // → repo call (3)
        service.findByUserIdAndContentType(userId, contentType); // → repo call (2)

        // Final verification — findById should be called 3 times, findByUserId... 2 times
        verify(repository, times(3)).findById(id); // <-- CHANGED FROM times(2) to times(3)
        verify(repository, times(2)).findByUserIdAndContentType(userId, contentType);
    }    

    @Test
    void deleteById_shouldEvictBothCaches() {
        clearCaches();
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String contentType = "article";

        GenericContentModel content = createSampleContent();
        content.setId(id);
        content.setUserId(userId);
        content.setType(contentType);

        when(repository.findById(id)).thenReturn(Optional.of(content));
        when(repository.existsById(id)).thenReturn(true);
        when(repository.findByUserIdAndContentType(userId, contentType))
                .thenReturn(List.of(content))
                .thenReturn(List.of()); // after delete

        service.getById(id);
        service.findByUserIdAndContentType(userId, contentType);

        verify(repository, times(1)).findById(id);
        verify(repository, times(1)).findByUserIdAndContentType(userId, contentType);

        doNothing().when(repository).deleteById(id);
        service.deleteById(id);

        when(repository.findById(id)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> service.getById(id));

        service.findByUserIdAndContentType(userId, contentType);

        verify(repository, times(2)).findById(id);      
        verify(repository, times(2)).findByUserIdAndContentType(userId, contentType);
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

    private void clearCaches() {
        Optional.ofNullable(cacheManager.getCache("genericContentById")).ifPresent(cache -> cache.clear());
        Optional.ofNullable(cacheManager.getCache("genericContentByUserAndType")).ifPresent(cache -> cache.clear());
    }

}