package com.dehold.contentmanager.ratelimiter.controller;

import com.dehold.contentmanager.ratelimiter.config.RateLimitConfig;
import com.dehold.contentmanager.ratelimiter.service.RateLimitConfigService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/rate-limits")
public class RateLimitAdminController {

    private final RateLimitConfigService service;

    public RateLimitAdminController(RateLimitConfigService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<RateLimitConfig>> list() {
        return ResponseEntity.ok(service.findAll());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RateLimitConfig> create(@RequestBody RateLimitConfig cfg) {
        RateLimitConfig created = service.create(cfg);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RateLimitConfig> update(@PathVariable UUID id, @RequestBody RateLimitConfig cfg) {
        RateLimitConfig updated = service.update(id, cfg);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
