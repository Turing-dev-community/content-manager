package com.dehold.contentmanager.validation.service;

import com.dehold.contentmanager.content.blogpost.model.BlogPost;
import com.dehold.contentmanager.validation.model.ValidationResult;
import com.dehold.contentmanager.validation.web.dto.BlogPostValidationRequest;
import com.dehold.contentmanager.validation.web.dto.ValidationReportDto;
import com.dehold.contentmanager.validation.web.dto.ValidationResponse;

import java.util.List;
import java.util.UUID;

public interface ValidationService {
    ValidationResponse validateBlogPost(BlogPostValidationRequest request);

    void createValidationResult(ValidationResult result);

    List<ValidationResult> findByUserId(UUID id);

    List<ValidationResult> runBlogPostValidation(UUID userId);

    ValidationReportDto generateValidationReport(UUID userId, boolean detailed);

    List<ValidationResult> runSupportResponseValidation(UUID userId);

    List<ValidationResult> runSupportRequestValidation(UUID userId);

    List<ValidationResult> runGenericContentValidation(UUID userId, String contentType);

    List<ValidationResult> validateBlogpost(BlogPost post);
}
