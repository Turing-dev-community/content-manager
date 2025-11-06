package com.dehold.contentmanager.validation.web;

import com.dehold.contentmanager.content.blogpost.model.BlogPost;
import com.dehold.contentmanager.validation.model.ValidationResult;
import com.dehold.contentmanager.validation.service.ValidationService;
import com.dehold.contentmanager.validation.web.dto.BlogPostValidationRequest;
import com.dehold.contentmanager.validation.web.dto.ValidationResponse;
import com.dehold.contentmanager.validation.web.dto.ValidationResultDto;
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
    public ResponseEntity<List<ValidationResponse>> validateBlogPosts(@RequestParam UUID userId) {
        List<ValidationResult> results = validationService.runBlogPostValidation(userId);
        List<ValidationResponse> response = results.stream()
                .map(ValidationResultDto::from)
                .map(dto -> new ValidationResponse(BlogPost.class.getSimpleName(), dto))
                .toList();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
