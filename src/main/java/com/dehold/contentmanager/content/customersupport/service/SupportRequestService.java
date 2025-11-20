package com.dehold.contentmanager.content.customersupport.service;

import com.dehold.contentmanager.content.customersupport.model.SupportRequest;
import com.dehold.contentmanager.content.customersupport.repository.SupportRequestRepository;
import com.dehold.contentmanager.exception.EntityNotFoundException;

import org.springframework.dao.DataAccessException;
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
        include = {TransientDataAccessException.class, DataAccessException.class},
        exclude = {EntityNotFoundException.class},
        maxAttempts = 4,
        backoff = @Backoff(delay = 500, multiplier = 2.0, random = true)
    )
    public List<SupportRequest> findAll() {
        return repository.findAll();
    }

    @Retryable(
        include = {TransientDataAccessException.class, DataAccessException.class},
        exclude = {EntityNotFoundException.class},
        maxAttempts = 4,
        backoff = @Backoff(delay = 500, multiplier = 2.0, random = true)
    )
    public SupportRequest findById(UUID id) {
        return repository.getById(id)
                .orElseThrow(() -> EntityNotFoundException.of("CustomerRequest", id.toString()));
    }

    @Recover
    public List<SupportRequest> recoverFindAll(TransientDataAccessException e) { 
        throw new ResponseStatusException(
            HttpStatus.SERVICE_UNAVAILABLE,
            "Support requests temporarily unavailable. Please try again later.",
            e
        );
    }

    @Recover
    public SupportRequest recoverFindById(TransientDataAccessException e, UUID id) {
        throw new ResponseStatusException(
            HttpStatus.SERVICE_UNAVAILABLE,
            "Failed to fetch support request. Please try again later.",
            e
        );
    }

    // Keep the optional version for internal use if needed
    public Optional<SupportRequest> findByIdOptional(UUID id) {
        return repository.getById(id);
    }

    public void createCustomerRequest(SupportRequest supportRequest) {
        repository.create(supportRequest);
    }

    public void updateCustomerRequest(SupportRequest supportRequest) {
        repository.update(supportRequest);
    }

    public void save(SupportRequest supportRequest) {
        repository.create(supportRequest);
    }

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
