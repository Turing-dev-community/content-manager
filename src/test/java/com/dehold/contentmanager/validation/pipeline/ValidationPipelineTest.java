package com.dehold.contentmanager.validation.pipeline;

import com.dehold.contentmanager.content.blogpost.model.BlogPost;
import com.dehold.contentmanager.validation.model.ValidationResult;
import com.dehold.contentmanager.validation.model.ValidationStepType;
import com.dehold.contentmanager.validation.step.LengthValidator;
import com.dehold.contentmanager.validation.step.PhoneNumberForbiddenValidator;
import com.dehold.contentmanager.validation.step.ValidationStep;
import com.dehold.contentmanager.validation.step.ValidationStepFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ValidationPipelineTest {

    @Test
    void givenValidContent_whenRunPipelineWithOneValidationStep_thenShouldReturnValid() {
        BlogPost post = new BlogPost(null, "Title", "Valid Content", null, null, null);
        ValidationStep<BlogPost> lengthValidator = new LengthValidator<>(BlogPost::getContent, "content", 5, 20);

        ValidationPipeline<BlogPost> pipeline = new ValidationPipelineBuilder<BlogPost>()
                .addStep(lengthValidator)
                .build();

        ValidationResult result = pipeline.run(post);
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void givenInvalidContent_whenRunPipelineWithOneValidationStep_thenShouldReturnError() {
        BlogPost post = new BlogPost(null, "Title", "Short", null, null, null);

        String fieldName = "content";
        ValidationStep<BlogPost> lengthValidator = new LengthValidator<>(BlogPost::getContent, fieldName, 10, 20);

        ValidationPipeline<BlogPost> pipeline = new ValidationPipelineBuilder<BlogPost>()
                .addStep(lengthValidator)
                .build();

        ValidationResult result = pipeline.run(post);
        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
        assertEquals(LengthValidator.ERROR_CODE, result.getErrors().getFirst().code());
        assertEquals(LengthValidator.errorMessageTooShort(fieldName), result.getErrors().getFirst().message());
    }

    @Test
    void givenValidContent_whenRunPipelineWithTwoValidationSteps_thenShouldReturnValid() {
        BlogPost post = new BlogPost(null, "Title", "Valid Content", null, null, null);

        ValidationStep<BlogPost> lengthValidator = new LengthValidator<>(BlogPost::getContent, "content", 5, 20);
        ValidationStepFactory factory = new ValidationStepFactory(null);
        ValidationStep<BlogPost> phoneNumberForbiddenValidator = factory.createValidationStep(ValidationStepType.PHONE_NUMBER_FORBIDDEN_VALIDATION, BlogPost::getContent, null, "content", null);

        ValidationPipeline<BlogPost> pipeline = new ValidationPipelineBuilder<BlogPost>()
                .addStep(lengthValidator)
                .addStep(phoneNumberForbiddenValidator)
                .build();

        ValidationResult result = pipeline.run(post);
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void givenInvalidContent_whenRunPipelineWithMultipleValidationSteps_thenShouldReturnError() {
        BlogPost post = new BlogPost(null, "Title", "Content with phone number: +1234567890", null, null, null);

        String fieldName = "content";
        ValidationStep<BlogPost> lengthValidator = new LengthValidator<>(BlogPost::getContent, fieldName, 5, 50);
        ValidationStepFactory factory = new ValidationStepFactory(null);
        ValidationStep<BlogPost> phoneNumberForbiddenValidator = factory.createValidationStep(ValidationStepType.PHONE_NUMBER_FORBIDDEN_VALIDATION, BlogPost::getContent, null, "content", null);

        ValidationPipeline<BlogPost> pipeline = new ValidationPipelineBuilder<BlogPost>()
                .addStep(lengthValidator)
                .addStep(phoneNumberForbiddenValidator)
                .build();

        ValidationResult result = pipeline.run(post);
        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
        assertEquals(PhoneNumberForbiddenValidator.ERROR_CODE, result.getErrors().getFirst().code());
        assertEquals(PhoneNumberForbiddenValidator.errorMessagePhoneNumberForbidden(fieldName),
                result.getErrors().getFirst().message());
    }
}