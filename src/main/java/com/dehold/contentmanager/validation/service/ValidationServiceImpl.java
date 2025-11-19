package com.dehold.contentmanager.validation.service;

import com.dehold.contentmanager.content.Content;
import com.dehold.contentmanager.content.blogpost.model.BlogPost;
import com.dehold.contentmanager.content.blogpost.service.BlogPostService;
import com.dehold.contentmanager.content.customersupport.model.SupportRequest;
import com.dehold.contentmanager.content.customersupport.model.SupportResponse;
import com.dehold.contentmanager.content.customersupport.repository.SupportRequestRepository;
import com.dehold.contentmanager.validation.model.ValidationStepType;
import com.dehold.contentmanager.validation.pipeline.ValidationPipeline;
import com.dehold.contentmanager.validation.pipeline.ValidationPipelineBuilder;
import com.dehold.contentmanager.validation.model.ValidationResult;
import com.dehold.contentmanager.validation.pipeline.ValidationPipelineFactory;
import com.dehold.contentmanager.validation.repository.ValidationResultRepository;
import com.dehold.contentmanager.validation.step.ValidationStep;
import com.dehold.contentmanager.validation.step.ValidationStepFactory;
import com.dehold.contentmanager.validation.web.dto.BlogPostValidationRequest;
import com.dehold.contentmanager.validation.web.dto.ValidationReportDto;
import com.dehold.contentmanager.validation.web.dto.ValidationResponse;
import com.dehold.contentmanager.validation.web.dto.ValidationResultDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.dehold.contentmanager.content.customersupport.service.SupportResponseService;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

@Service
public class ValidationServiceImpl implements ValidationService {

    @Autowired
    private final ValidationPipelineFactory validationPipelineFactory;

    @Autowired
    private final ValidationResultRepository validationResultRepository;

    @Autowired
    private final SupportResponseService supportResponseService;

    @Autowired
    private final BlogPostService blogPostService;

    private final SupportRequestRepository supportRequestRepository;

    public ValidationServiceImpl(ValidationResultRepository validationResultRepository,
                                 ValidationPipelineFactory validationPipelineFactory, BlogPostService blogPostService, SupportResponseService supportResponseService, SupportRequestRepository supportRequestRepository) {
        this.supportResponseService = supportResponseService;
        this.blogPostService = blogPostService;
        this.validationPipelineFactory = validationPipelineFactory;
        this.validationResultRepository = validationResultRepository;
        this.supportRequestRepository = supportRequestRepository;
    }

    @Override
    public ValidationResponse validateBlogPost(BlogPostValidationRequest request) {
        ValidationStepFactory factory = new ValidationStepFactory(null);
        ValidationStep<BlogPost> titleLengthValidator = getTitleLengthValidator(request, factory);
        ValidationStep<BlogPost> contentLengthValidator = getBlogPostLengthValidator(request, factory);
        ValidationPipeline<BlogPost> pipeline = new ValidationPipelineBuilder<BlogPost>()
                .addStep(titleLengthValidator)
                .addStep(contentLengthValidator)
                .build();
        ValidationResult result = pipeline.run(request.getBlogPost());

        this.createValidationResult(result);

        ValidationResultDto resultDto = ValidationResultDto.from(result);
        return new ValidationResponse(BlogPost.class.getSimpleName(), resultDto);
    }

    @Override
    public List<ValidationResult> runBlogPostValidation(UUID userId) {
        List<BlogPost> blogPosts = blogPostService.getBlogPostsByUserId(userId);
        List<ValidationResult> allResults = new LinkedList<>();
        for(BlogPost blogPost : blogPosts) {
            var results = runValidationPipelinesForBlogPost(userId, blogPost);
            allResults.addAll(results);
        }
        return allResults;
    }

    @Override
    public ValidationReportDto generateValidationReport(UUID userId) {
        List<ValidationResult> results = validationResultRepository.findByUserId(userId);
        int totalErrorCount = 0;
        Map<String, Integer> codeCounts = new HashMap<>();
        for (ValidationResult result : results) {
            totalErrorCount += result.getErrors().size();
            result.getErrors().forEach(error -> codeCounts.merge(error.code(), 1, Integer::sum));
        }
        Map<String, String> errorCodeToErrorCount = new HashMap<>();
        codeCounts.forEach((k, v) -> errorCodeToErrorCount.put(k, String.valueOf(v)));
        return new ValidationReportDto(totalErrorCount, errorCodeToErrorCount);
    }

    private List<ValidationResult> runValidationPipelinesForBlogPost(UUID userId, BlogPost blogPost) {
        List<ValidationPipeline<BlogPost>> pipelines =
                validationPipelineFactory.createValidationPipelineForUserAndContentType(userId,
                        "blogpost");
        List<ValidationResult> results = new LinkedList<>();
        collectResults(blogPost, pipelines, results);
        persistsResults(results);
        return results;
    }

    private <T extends Content> void collectResults(T content, List<ValidationPipeline<T>> pipelines, List<ValidationResult> results) {
        for (ValidationPipeline<T> pipeline : pipelines) {
            ValidationResult result = pipeline.run(content);
            results.add(result);
        }
    }

    private void persistsResults(List<ValidationResult> results) {
        for(ValidationResult result : results) {
            this.createValidationResult(result);
        }
    }

    private static ValidationStep<BlogPost> getBlogPostLengthValidator(BlogPostValidationRequest request, ValidationStepFactory factory) {
        return factory.createValidationStep(ValidationStepType.LENGTH_VALIDATION, BlogPost::getContent,
                Map.of("minLength", String.valueOf(request.getContentMinLength()), "maxLength",
                        String.valueOf(request.getContentMaxLength())), "content", null);
    }

    private static ValidationStep<BlogPost> getTitleLengthValidator(BlogPostValidationRequest request, ValidationStepFactory factory) {
        return factory.createValidationStep(ValidationStepType.LENGTH_VALIDATION, BlogPost::getTitle,
                Map.of("minLength", String.valueOf(request.getTitleMinLength()), "maxLength",
                        String.valueOf(request.getTitleMaxLength())), "title", null);
    }

    @Override
    public void createValidationResult(ValidationResult result) {
        validationResultRepository.create(result);
    }

    @Override
    public List<ValidationResult> findByUserId(UUID id) {
        return validationResultRepository.findByUserId(id);
    }

    @Override
    public List<ValidationResult> runSupportResponseValidation(UUID userId) {
        List<SupportResponse> responses = supportResponseService.getSupportResponsesByUserId(userId);
        List<ValidationResult> allResults = new LinkedList<>();
        for (SupportResponse response : responses) {
            List<ValidationResult> results = runValidationPipelinesForSupportResponse(userId, response);
            allResults.addAll(results);
        }
        return allResults;
    }

    private List<ValidationResult> runValidationPipelinesForSupportResponse(UUID userId, SupportResponse response) {
        List<ValidationPipeline<SupportResponse>> pipelines =
                validationPipelineFactory.createValidationPipelineForUserAndContentType(userId,
                        "supportresponse");
        List<ValidationResult> results = new LinkedList<>();
        collectResults(response, pipelines, results);
        persistsResults(results);
        return results;
    }

    @Override
    public List<ValidationResult> runSupportRequestValidation(UUID userId) {
        // Fetch all support requests for the given user
        List<SupportRequest> supportRequests = supportRequestRepository.findByUserId(userId);

        List<ValidationResult> allResults = new LinkedList<>();
        // For each support request, run all validation pipelines configured for "supportrequest"
        for (SupportRequest supportRequest : supportRequests) {
            List<ValidationPipeline<SupportRequest>> pipelines = validationPipelineFactory.createValidationPipelineForUserAndContentType(userId, "supportrequest");

            for (ValidationPipeline<SupportRequest> pipeline : pipelines) {
                ValidationResult result = pipeline.run(supportRequest);
                allResults.add(result);
            }
        }
        //persist the results
        persistsResults(allResults);
        return allResults;
    }

}
