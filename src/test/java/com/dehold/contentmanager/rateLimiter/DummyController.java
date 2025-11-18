package com.dehold.contentmanager.rateLimiter;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class DummyController {

    @GetMapping("/api/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("OK");
    }
}