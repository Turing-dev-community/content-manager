package com.dehold.contentmanager.validation.service;

import com.dehold.contentmanager.content.blogpost.model.BlogPost;
import com.dehold.contentmanager.validation.model.ValidationStepType;
import com.dehold.contentmanager.validation.pipeline.ValidationPipeline;
import com.dehold.contentmanager.validation.pipeline.ValidationPipelineBuilder;
import com.dehold.contentmanager.validation.model.ValidationResult;
import com.dehold.contentmanager.validation.repository.ValidationResultRepository;
import com.dehold.contentmanager.validation.step.LengthValidator;
import com.dehold.contentmanager.validation.step.ValidationStep;
import com.dehold.contentmanager.validation.step.ValidationStepFactory;
import com.dehold.contentmanager.validation.web.dto.BlogPostValidationRequest;
import com.dehold.contentmanager.validation.web.dto.ValidationResponse;
import com.dehold.contentmanager.validation.web.dto.ValidationResultDto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ValidationServiceImpl implements ValidationService {

    private final ValidationResultRepository validationResultRepository;

    public ValidationServiceImpl(ValidationResultRepository validationResultRepository) {
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
