package com.dehold.contentmanager.validation.service;

import com.dehold.contentmanager.validation.model.ValidationResult;
import com.dehold.contentmanager.validation.web.dto.BlogPostValidationRequest;
import com.dehold.contentmanager.validation.web.dto.ValidationResponse;

import java.util.List;
import java.util.UUID;

public interface ValidationService {
    ValidationResponse validateBlogPost(BlogPostValidationRequest request);

    void createValidationResult(ValidationResult result);

    List<ValidationResult> findByUserId(UUID id);

    public List<ValidationResult> runBlogPostValidation(UUID userId);
}
