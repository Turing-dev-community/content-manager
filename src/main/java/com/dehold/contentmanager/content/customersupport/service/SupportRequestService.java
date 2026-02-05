package com.dehold.contentmanager.content.customersupport.service;

import com.dehold.contentmanager.content.customersupport.model.SupportRequest;
import com.dehold.contentmanager.content.customersupport.repository.SupportRequestRepository;
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
import java.util.Optional;
import java.util.UUID;
import java.util.ArrayList;

@Service
public class SupportRequestService {
    private final SupportRequestRepository repository;

    public SupportRequestService(SupportRequestRepository repository) {
        this.repository = repository;
    }
    
    @Retryable(
        include = {TransientDataAccessException.class},
        exclude = {EntityNotFoundException.class},
        maxAttempts = 4,
        backoff = @Backoff(delay = 500, multiplier = 2.0, random = true)
    )
    @Cacheable(value = "supportRequests")
    public List<SupportRequest> findAll() {
        return repository.findAll();
    }

    @Retryable(
        include = {TransientDataAccessException.class},
        exclude = {EntityNotFoundException.class},
        maxAttempts = 4,
        backoff = @Backoff(delay = 500, multiplier = 2.0, random = true)
    )
    @Cacheable(value = "supportRequestById", key = "#id")
    public SupportRequest findById(UUID id) {
        return repository.getById(id)
                .orElseThrow(() -> EntityNotFoundException.of("CustomerRequest", id.toString()));
    }

    @Recover
    public List<SupportRequest> recoverFindAll(TransientDataAccessException e) { 
        throw new ResponseStatusException(
            HttpStatus.SERVICE_UNAVAILABLE,
            "The support request system is temporarily unavailable due to high load. Please try again shortly.",
            e
        );
    }

    @Recover
    public SupportRequest recoverFindById(TransientDataAccessException e, UUID id) {
        throw new ResponseStatusException(
            HttpStatus.SERVICE_UNAVAILABLE,
            "The support request system is temporarily unavailable due to high load. Please try again shortly.",
            e
        );
    }

    @Recover
    public SupportRequest recoverFindById(EntityNotFoundException e, UUID id) {
        throw e; 
    }

    // Keep the optional version for internal use if needed
    public Optional<SupportRequest> findByIdOptional(UUID id) {
        return repository.getById(id);
    }

    @CacheEvict(value = "supportRequests", allEntries = true)
    public void createCustomerRequest(SupportRequest supportRequest) {
        repository.create(supportRequest);
    }

    @CacheEvict(value = "supportRequests", key = "#supportRequest.id")
    public void updateCustomerRequest(SupportRequest supportRequest) {
        repository.update(supportRequest);
    }

    public void save(SupportRequest supportRequest) {
        repository.create(supportRequest);
    }

    @CacheEvict(value = {"supportRequests", "supportRequestById"}, key = "#id")
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }

    public void addSubscriber(UUID requestId, UUID userId) {
        SupportRequest req = findById(requestId);
        List<UUID> subs = req.getSubscribers();
        if (subs == null) {
            subs = new ArrayList<>();
        }
        if (!subs.contains(userId)) {
            subs.add(userId);
            req.setSubscribers(subs);
            repository.update(req);
        }
    }

    public void removeSubscriber(UUID requestId, UUID userId) {
        SupportRequest req = findById(requestId);
        List<UUID> subs = req.getSubscribers();
        if (subs == null) return;
        if (subs.remove(userId)) {
            req.setSubscribers(subs);
            repository.update(req);
        }
    }
}
