package com.dehold.contentmanager.content.customersupport.service;

import com.dehold.contentmanager.content.customersupport.model.SupportRequest;
import com.dehold.contentmanager.content.customersupport.model.SupportResponse;
import com.dehold.contentmanager.content.customersupport.repository.SupportResponseRepository;
import com.dehold.contentmanager.exception.EntityNotFoundException;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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
    @Retryable(
            include = {TransientDataAccessException.class},
            exclude = {EntityNotFoundException.class},
            maxAttempts = 4,
            backoff = @Backoff(delay = 500, multiplier = 2.0, random = true)
    )
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
    @Retryable(
            include = {TransientDataAccessException.class},
            exclude = {EntityNotFoundException.class},
            maxAttempts = 4,
            backoff = @Backoff(delay = 500, multiplier = 2.0, random = true)
    )
    @Cacheable(value = "supportResponsesByUserId", key = "#userId")
    public List<SupportResponse> getSupportResponsesByUserId(UUID userId) {
        return repository.getSupportResponsesByUserId(userId);
    }

    @Recover
    public SupportResponse recoverGetSupportResponses(TransientDataAccessException e) {
        throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "The support request system is temporarily unavailable due to high load. Please try again shortly.",
                e
        );
    }

    @Recover
    public List<SupportResponse> recoverGetSupportResponsesByUserId(TransientDataAccessException e, UUID id) {
        throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "The support request system is temporarily unavailable due to high load. Please try again shortly.",
                e
        );
    }

    @Recover
    public SupportResponse recoverNotFound(EntityNotFoundException e, UUID id) {
        throw e;
    }

    @Recover
    public List<SupportResponse> recoverNotFoundList(EntityNotFoundException e, UUID id) {
        throw e;
    }

}
