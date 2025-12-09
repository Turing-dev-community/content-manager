package com.dehold.contentmanager.content.customersupport.service;

import com.dehold.contentmanager.content.customersupport.model.SupportRequest;
import com.dehold.contentmanager.content.customersupport.repository.SupportRequestRepository;
import com.dehold.contentmanager.content.customersupport.util.FailedIdTracker;
import com.dehold.contentmanager.content.customersupport.util.RetryContext;
import com.dehold.contentmanager.content.customersupport.web.dto.PartialResultResponse;
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
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SupportRequestService {
    private final SupportRequestRepository repository;
    private final FailedIdTracker failedIdTracker;
    
    private static final int MAX_RETRY_ATTEMPTS = 4;
    private static final long INITIAL_BACKOFF_MS = 500;
    private static final double BACKOFF_MULTIPLIER = 2.0;

    public SupportRequestService(SupportRequestRepository repository) {
        this.repository = repository;
        this.failedIdTracker = new FailedIdTracker();
    }
    
    /**
     * Constructor for testing with custom FailedIdTracker
     */
    public SupportRequestService(SupportRequestRepository repository, FailedIdTracker failedIdTracker) {
        this.repository = repository;
        this.failedIdTracker = failedIdTracker;
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

    @CacheEvict(value = {"supportRequests", "supportRequestById"}, key = "#supportRequest.id")
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
    
    /**
     * Find all requests with retry logic and partial result handling
     * Returns PartialResultResponse containing successful results and failed IDs
     */
    public PartialResultResponse<SupportRequest> findAllWithRetry() {
        RetryContext retryContext = new RetryContext(MAX_RETRY_ATTEMPTS, INITIAL_BACKOFF_MS, BACKOFF_MULTIPLIER);
        List<SupportRequest> successfulResults = new ArrayList<>();
        
        while (retryContext.canRetry()) {
            try {
                successfulResults = repository.findAll();
                failedIdTracker.clear();
                
                // Successful fetch, build response with no failures
                return new PartialResultResponse<>(
                    successfulResults,
                    new HashMap<>(),
                    buildRetryInfo(retryContext, 0)
                );
            } catch (TransientDataAccessException e) {
                retryContext.recordAttempt(e);
                
                if (retryContext.canRetry()) {
                    try {
                        long backoffMs = retryContext.getNextBackoffDelayMs();
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        // All retries exhausted, return what we have
        throw new ResponseStatusException(
            HttpStatus.SERVICE_UNAVAILABLE,
            "The support request system is temporarily unavailable due to high load. Please try again shortly. " +
            "Attempted " + retryContext.getAttemptCount() + " times.",
            retryContext.getLastException()
        );
    }
    
    /**
     * Find request by ID with retry logic and partial result handling
     * Returns PartialResultResponse containing the successful result or failed ID info
     */
    public PartialResultResponse<SupportRequest> findByIdWithRetry(UUID id) {
        RetryContext retryContext = new RetryContext(MAX_RETRY_ATTEMPTS, INITIAL_BACKOFF_MS, BACKOFF_MULTIPLIER, id);
        
        while (retryContext.canRetry()) {
            try {
                Optional<SupportRequest> result = repository.getById(id);
                
                if (result.isPresent()) {
                    failedIdTracker.removeFailure(id);
                    return new PartialResultResponse<>(
                        List.of(result.get()),
                        new HashMap<>(),
                        buildRetryInfo(retryContext, 0)
                    );
                } else {
                    throw EntityNotFoundException.of("CustomerRequest", id.toString());
                }
            } catch (EntityNotFoundException e) {
                // Not retryable - entity doesn't exist
                throw e;
            } catch (TransientDataAccessException e) {
                retryContext.recordAttempt(e);
                failedIdTracker.recordFailure(id, e.getMessage(), 
                    MAX_RETRY_ATTEMPTS - retryContext.getAttemptCount());
                
                if (retryContext.canRetry()) {
                    try {
                        long backoffMs = retryContext.getNextBackoffDelayMs();
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        // All retries exhausted, return partial response with failure info
        Map<UUID, PartialResultResponse.FailedIdInfo> failedIds = new HashMap<>();
        failedIds.put(id, new PartialResultResponse.FailedIdInfo(
            id,
            "Database access failed after " + retryContext.getAttemptCount() + " retry attempts",
            0
        ));
        
        PartialResultResponse<SupportRequest> response = new PartialResultResponse<>(
            new ArrayList<>(),
            failedIds,
            buildRetryInfo(retryContext, 1)
        );
        response.setPartial(true);
        
        return response;
    }
    
    /**
     * Find multiple IDs with retry logic for failed items
     * Returns PartialResultResponse with successful items and failed IDs
     */
    public PartialResultResponse<SupportRequest> findByIdsWithRetry(List<UUID> ids) {
        List<SupportRequest> successfulResults = new ArrayList<>();
        Map<UUID, PartialResultResponse.FailedIdInfo> failedIds = new HashMap<>();
        
        for (UUID id : ids) {
            try {
                PartialResultResponse<SupportRequest> response = findByIdWithRetry(id);
                if (response.getSuccessfulResults() != null && !response.getSuccessfulResults().isEmpty()) {
                    successfulResults.addAll(response.getSuccessfulResults());
                } else if (response.getFailedIds() != null) {
                    failedIds.putAll(response.getFailedIds());
                }
            } catch (EntityNotFoundException e) {
                // Entity not found, skip it
                failedIds.put(id, new PartialResultResponse.FailedIdInfo(
                    id,
                    "Entity not found",
                    0
                ));
            }
        }
        
        PartialResultResponse<SupportRequest> response = new PartialResultResponse<>(
            successfulResults,
            failedIds
        );
        response.setPartial(!failedIds.isEmpty());
        
        return response;
    }
    
    /**
     * Get failed ID tracker for monitoring purposes
     */
    public FailedIdTracker getFailedIdTracker() {
        return failedIdTracker;
    }
    
    /**
     * Clear all tracked failures
     */
    public void clearFailedIdTracker() {
        failedIdTracker.clear();
    }
    
    private PartialResultResponse.RetryInfo buildRetryInfo(RetryContext context, int failureCount) {
        long nextBackoffMs = context.canRetry() ? context.getNextBackoffDelayMs() : 0;
        
        return new PartialResultResponse.RetryInfo(
            nextBackoffMs,
            "Exponential backoff with multiplier " + BACKOFF_MULTIPLIER,
            MAX_RETRY_ATTEMPTS,
            context.getAttemptCount()
        );
    }
}
