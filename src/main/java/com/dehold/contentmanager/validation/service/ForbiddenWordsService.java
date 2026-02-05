package com.dehold.contentmanager.validation.service;

import com.dehold.contentmanager.exception.EntityNotFoundException;
import com.dehold.contentmanager.service.IService;
import com.dehold.contentmanager.validation.model.ForbiddenWords;
import com.dehold.contentmanager.validation.repository.ForbiddenWordsRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

@Service
public class ForbiddenWordsService implements IService<ForbiddenWords> {
    private final ForbiddenWordsRepository repository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ForbiddenWordsService(ForbiddenWordsRepository repository) {
        this.repository = repository;
    }

    @Override
    public ForbiddenWords create(ForbiddenWords entity) {
        repository.save(entity);
        return entity;
    }

    @Override
    public ForbiddenWords findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> EntityNotFoundException.of("ForbiddenWords", id.toString()));
    }

    @Override
    public List<ForbiddenWords> findAll() {
        List<ForbiddenWords> forbiddenWords = repository.findAll();
        return addDefaultForbiddenWords(forbiddenWords);
    }

    public List<ForbiddenWords> findByUserIdAndContentType(UUID userId, String contentType) {
        ForbiddenWords customForbiddenWords = repository.findByUserIdAndContentType(userId, contentType);
        return addDefaultForbiddenWords(List.of(customForbiddenWords));
    }

    private List<ForbiddenWords> addDefaultForbiddenWords(List<ForbiddenWords> customForbiddenWords) {
        if (customForbiddenWords == null || customForbiddenWords.isEmpty()) {
            return customForbiddenWords;
        }
        ForbiddenWords defaultForbiddenWords =
                new ForbiddenWords(
                        null,
                        null,
                        "Default Forbidden Words provided by the system. These are not editable or deletable.",
                        "any",
                        "any",
                        getDefaultForbiddenWords());
        customForbiddenWords.add(defaultForbiddenWords);
        return customForbiddenWords;
    }

    private LinkedHashSet<String> getDefaultForbiddenWords() {
        ClassPathResource resource = new ClassPathResource("forbiddenWords.json");
        if (!resource.exists()) {
            throw new IllegalStateException("forbiddenWords.json not found on classpath");
        }
        try (InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readValue(inputStream, new TypeReference<>() {});
        } catch(IOException e) {
            throw new IllegalStateException("Failed to load default forbidden words", e);
        }
    }

    @Override
    public ForbiddenWords update(UUID id, ForbiddenWords entity) {
        ForbiddenWords existing = findById(id);

        entity.setCreatedAt(existing.getCreatedAt());
        entity.setUpdatedAt(java.time.Instant.now());

        repository.save(entity);
        return entity;
    }

    @Override
    public void delete(UUID id) {
        repository.findById(id).ifPresent(repository::delete);
    }
}
