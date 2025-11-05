package com.dehold.contentmanager.validation.web;

import com.dehold.contentmanager.validation.model.ValidationResult;
import com.dehold.contentmanager.validation.service.ValidationService;
import com.dehold.contentmanager.validation.web.dto.BlogPostValidationRequest;
import com.dehold.contentmanager.validation.web.dto.ValidationResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/validate")
public class ValidationController {
    private final ValidationService validationService;

    public ValidationController(ValidationService validationService) {
        this.validationService = validationService;
    }

    @PostMapping("/blogpost")
    public ResponseEntity<ValidationResponse> validateBlogPost(@RequestBody BlogPostValidationRequest request) {
        ValidationResponse result = validationService.validateBlogPost(request);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/validate-blogposts")
    public ResponseEntity<List<ValidationResult>> validateBlogPosts(@RequestParam UUID userId) {
        List<ValidationResult> results = validationService.runBlogPostValidation(userId);
        return new ResponseEntity<>(results, HttpStatus.OK);
    }
}
