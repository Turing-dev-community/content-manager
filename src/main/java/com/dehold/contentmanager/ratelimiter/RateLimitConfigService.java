package com.dehold.contentmanager.ratelimiter;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitConfigService {

    private final RateLimitConfigRepository repo;

    // cache for fast lookup: list of configs indexed by pathPattern (we prefer longest prefix match)
    // Keep a simple thread-safe snapshot cache that can be refreshed after changes.
    private volatile List<RateLimitConfig> cachedConfigs = List.of();

    public RateLimitConfigService(RateLimitConfigRepository repo) {
        this.repo = repo;
        reloadCache();
    }

    public void reloadCache() {
        this.cachedConfigs = repo.findAll();
        // sort descending by path length so we can find the most specific prefix quickly
        this.cachedConfigs.sort((a, b) -> Integer.compare(b.getPathPattern().length(), a.getPathPattern().length()));
    }

    public RateLimitConfig create(RateLimitConfig cfg) {
        cfg.setId(UUID.randomUUID());
        cfg.setCreatedAt(Instant.now());
        cfg.setUpdatedAt(Instant.now());
        repo.insert(cfg);
        reloadCache();
        return cfg;
    }

    public Optional<RateLimitConfig> findByPathExact(String path) {
        return repo.findByPathPattern(path);
    }

    public List<RateLimitConfig> findAll() {
        return repo.findAll();
    }

    public Optional<RateLimitConfig> findBestMatchForPath(String requestPath) {
        for (RateLimitConfig cfg : cachedConfigs) {
            String pattern = cfg.getPathPattern();
            if (requestPath.equals(pattern) || requestPath.startsWith(pattern + "/") || requestPath.startsWith(pattern)) {
                return Optional.of(cfg);
            }
        }
        return Optional.empty();
    }

    public Optional<RateLimitConfig> findById(UUID id) {
        return repo.findById(id);
    }

    public RateLimitConfig update(UUID id, RateLimitConfig updated) {
        RateLimitConfig cfg = repo.findById(id).orElseThrow(() -> new NoSuchElementException("RateLimitConfig not found"));
        cfg.setCapacity(updated.getCapacity());
        cfg.setRefillTokens(updated.getRefillTokens());
        cfg.setRefillIntervalMillis(updated.getRefillIntervalMillis());
        cfg.setUpdatedAt(Instant.now());
        repo.update(cfg);
        reloadCache();
        return cfg;
    }

    public void delete(UUID id) {
        repo.delete(id);
        reloadCache();
    }
}
