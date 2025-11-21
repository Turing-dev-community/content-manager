package com.dehold.contentmanager.content.customersupport.service;

import com.dehold.contentmanager.content.customersupport.model.SupportResponse;
import com.dehold.contentmanager.content.customersupport.repository.SupportResponseRepository;
import com.dehold.contentmanager.exception.EntityNotFoundException;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class SupportResponseServiceImpl implements SupportResponseService {
    private final SupportResponseRepository repository;

    public SupportResponseServiceImpl(SupportResponseRepository repository) {
        this.repository = repository;
    }

    @Override
    @CacheEvict(value = "supportResponsesByUserId", allEntries = true)
    public void createSupportResponse(SupportResponse response) {
        repository.create(response);
    }

    @Override
    @Cacheable(value = "supportResponseById", key = "#id")
    public SupportResponse getSupportResponse(UUID id) {
        return repository.getById(id)
                .orElseThrow(() -> EntityNotFoundException.of("SupportResponse", id.toString()));
    }

    @Override
    @CacheEvict(value = {"supportResponseById", "supportResponsesByUserId"}, key = "#response.id", allEntries = true)
    public void updateSupportResponse(SupportResponse response) {
        repository.update(response);
    }

    @Override
    @CacheEvict(value = {"supportResponseById", "supportResponsesByUserId"}, key = "#id", allEntries = true)
    public void deleteSupportResponse(UUID id) {
        repository.delete(id);
    }

    @Override
    @Cacheable(value = "supportResponsesByUserId", key = "#userId")
    public List<SupportResponse> getSupportResponsesByUserId(UUID userId) {
        return repository.getSupportResponsesByUserId(userId);
    }
}
