package com.dehold.contentmanager.validation.service;

import com.dehold.contentmanager.content.Content;
import com.dehold.contentmanager.content.blogpost.model.BlogPost;
import com.dehold.contentmanager.content.blogpost.service.BlogPostService;
import com.dehold.contentmanager.validation.model.ContentTypeRegistry;
import com.dehold.contentmanager.validation.model.ValidationStepType;
import com.dehold.contentmanager.validation.pipeline.ValidationPipeline;
import com.dehold.contentmanager.validation.pipeline.ValidationPipelineBuilder;
import com.dehold.contentmanager.validation.model.ValidationResult;
import com.dehold.contentmanager.validation.pipeline.ValidationPipelineFactory;
import com.dehold.contentmanager.validation.pipeline.ValidationPipelineImpl;
import com.dehold.contentmanager.validation.repository.ValidationResultRepository;
import com.dehold.contentmanager.validation.step.LengthValidator;
import com.dehold.contentmanager.validation.step.ValidationStep;
import com.dehold.contentmanager.validation.step.ValidationStepFactory;
import com.dehold.contentmanager.validation.web.dto.BlogPostValidationRequest;
import com.dehold.contentmanager.validation.web.dto.ValidationResponse;
import com.dehold.contentmanager.validation.web.dto.ValidationResultDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ValidationServiceImpl implements ValidationService {

    @Autowired
    private final ValidationPipelineFactory validationPipelineFactory;

    @Autowired
    private final ValidationResultRepository validationResultRepository;

    @Autowired
    private final BlogPostService blogPostService;

    public ValidationServiceImpl(ValidationResultRepository validationResultRepository,
                                 ValidationPipelineFactory validationPipelineFactory, BlogPostService blogPostService) {
        this.blogPostService = blogPostService;
        this.validationPipelineFactory = validationPipelineFactory;
        this.validationResultRepository = validationResultRepository;
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

    private List<ValidationResult> runValidationPipelinesForBlogPost(UUID userId, BlogPost blogPost) {
        List<ValidationPipeline<BlogPost>> pipelines =
                validationPipelineFactory.createValidationPipelineForUserAndContentType(userId,
                        "blogpost");
        List<ValidationResult> results = new LinkedList<>();
        for(ValidationPipeline<BlogPost> pipeline : pipelines) {
            ValidationResult result = pipeline.run(blogPost);
            validationResultRepository.create(result);
            results.add(result);
        }
        return results;
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
}
