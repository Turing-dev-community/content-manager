package com.dehold.contentmanager.validation.service;

import com.dehold.contentmanager.content.blogpost.model.BlogPost;
import com.dehold.contentmanager.validation.model.ValidationError;
import com.dehold.contentmanager.validation.model.ValidationResult;
import com.dehold.contentmanager.validation.repository.ValidationResultRepository;
import com.dehold.contentmanager.validation.step.ForbiddenWordValidator;
import com.dehold.contentmanager.validation.step.LengthValidator;
import com.dehold.contentmanager.validation.web.dto.BlogPostValidationRequest;
import com.dehold.contentmanager.validation.web.dto.ValidationReportDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.List;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ValidationServiceTest {

    @Mock
    private ValidationResultRepository repository;

    @InjectMocks
    private ValidationServiceImpl validationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }


    @Test
    void givenValidContent_whenServiceValidates_thenValidationResultRepositoryIsCalled() {
        BlogPostValidationRequest blogPostValidationRequest = new BlogPostValidationRequest(5, 100, 20, 500,
                new BlogPost(UUID.randomUUID(), "Valid Title", "This is a valid content for the blog post.",
                        Instant.now(), Instant.now(), UUID.randomUUID()));
        validationService.validateBlogPost(blogPostValidationRequest);

        verify(repository, times(1)).create(any(ValidationResult.class));
    }

    @Test
    void givenInvalidContent_whenServiceValidates_thenValidationResultRepositoryIsCalled() {
        BlogPostValidationRequest blogPostValidationRequest = new BlogPostValidationRequest(50, 100, 20, 500,
                new BlogPost(UUID.randomUUID(), "Invalid Title: TOO SHORT", "This is a valid content for the blog " +
                        "post.",
                        Instant.now(), Instant.now(), UUID.randomUUID()));
        validationService.validateBlogPost(blogPostValidationRequest);

        verify(repository, times(1)).create(any(ValidationResult.class));
    }

    @Test
    void givenContent_whenServiceValidates_thenValidationResultIsCreatedWithRightValues() {
        BlogPostValidationRequest blogPostValidationRequest = new BlogPostValidationRequest(5, 100, 20, 500,
                new BlogPost(UUID.randomUUID(), "Valid Title", "This is a valid content for the blog post.",
                        Instant.now(), Instant.now(), UUID.randomUUID()));
        validationService.validateBlogPost(blogPostValidationRequest);

        ArgumentCaptor<ValidationResult> captor = ArgumentCaptor.forClass(ValidationResult.class);
        verify(repository, times(1)).create(captor.capture());

        ValidationResult captureResult = captor.getValue();
        assertNotNull(captureResult);
        assertEquals(BlogPost.class.getSimpleName(), captureResult.getContentType());
        assertEquals(blogPostValidationRequest.getBlogPost().getId(), captureResult.getContentId());
        assertTrue(captureResult.isValid());
    }

    @Test
    void givenOneValidationResultWithoutErrors_whenReport_thenShouldReturnNoErrors() {
        UUID userId = UUID.randomUUID();
        ValidationResult result = ValidationResult.valid(BlogPost.class.getSimpleName(), UUID.randomUUID(), userId);
        when(repository.findByUserId(userId)).thenReturn(List.of(result));

        ValidationReportDto report = validationService.generateValidationReport(userId);
        assertNotNull(report);
        assertEquals(0, report.getTotalErrorCount());
        assertTrue(report.getErrorCodeToErrorCount().isEmpty());
    }

    @Test
    void givenOneValidationResultWithSeveralErrors_whenReport_thenShouldReturnRightErrors() {
        UUID userId = UUID.randomUUID();
        List<ValidationError> errors = List.of(
                new ValidationError(LengthValidator.ERROR_CODE, LengthValidator.errorMessageTooShort("title")),
                new ValidationError(LengthValidator.ERROR_CODE, LengthValidator.errorMessageTooShort("content")),
                new ValidationError(ForbiddenWordValidator.ERROR_CODE, "Forbidden word 'spam'")
        );
        ValidationResult result = ValidationResult.invalid(BlogPost.class.getSimpleName(), UUID.randomUUID(), userId, errors);
        when(repository.findByUserId(userId)).thenReturn(List.of(result));

        ValidationReportDto report = validationService.generateValidationReport(userId);
        assertNotNull(report);
        assertEquals(3, report.getTotalErrorCount());
        assertEquals("2", report.getErrorCodeToErrorCount().get(LengthValidator.ERROR_CODE));
        assertEquals("1", report.getErrorCodeToErrorCount().get(ForbiddenWordValidator.ERROR_CODE));
        assertEquals(2, report.getErrorCodeToErrorCount().size());
    }

    @Test
    void givenSeveralValidationResultsWithoutErrors_whenReport_thenShouldReturnZeroErrors() {
        UUID userId = UUID.randomUUID();
        ValidationResult result1 = ValidationResult.valid(BlogPost.class.getSimpleName(), UUID.randomUUID(), userId);
        ValidationResult result2 = ValidationResult.valid(BlogPost.class.getSimpleName(), UUID.randomUUID(), userId);
        ValidationResult result3 = ValidationResult.valid(BlogPost.class.getSimpleName(), UUID.randomUUID(), userId);
        when(repository.findByUserId(userId)).thenReturn(List.of(result1, result2, result3));

        ValidationReportDto report = validationService.generateValidationReport(userId);
        assertNotNull(report);
        assertEquals(0, report.getTotalErrorCount());
        assertTrue(report.getErrorCodeToErrorCount().isEmpty());
    }

    @Test
    void givenSeveralValidationResultsWithErrors_whenReport_thenShouldReturnRightErrors() {
        UUID userId = UUID.randomUUID();
        List<ValidationError> errors1 = List.of(
                new ValidationError(LengthValidator.ERROR_CODE, LengthValidator.errorMessageTooShort("title")),
                new ValidationError(ForbiddenWordValidator.ERROR_CODE, "Forbidden word 'spam'")
        );
        List<ValidationError> errors2 = List.of(
                new ValidationError(LengthValidator.ERROR_CODE, LengthValidator.errorMessageTooShort("content"))
        );
        List<ValidationError> errors3 = List.of(
                new ValidationError(LengthValidator.ERROR_CODE, LengthValidator.errorMessageTooShort("title")),
                new ValidationError(LengthValidator.ERROR_CODE, LengthValidator.errorMessageTooShort("content")),
                new ValidationError(ForbiddenWordValidator.ERROR_CODE, "Forbidden word 'ads'")
        );
        ValidationResult result1 = ValidationResult.invalid(BlogPost.class.getSimpleName(), UUID.randomUUID(), userId,
                errors1);
        ValidationResult result2 = ValidationResult.invalid(BlogPost.class.getSimpleName(), UUID.randomUUID(), userId, errors2);
        ValidationResult result3 = ValidationResult.invalid(BlogPost.class.getSimpleName(), UUID.randomUUID(), userId, errors3);
        when(repository.findByUserId(userId)).thenReturn(List.of(result1, result2, result3));

        ValidationReportDto report = validationService.generateValidationReport(userId);
        assertNotNull(report);
        assertEquals(6, report.getTotalErrorCount());
        assertEquals("4", report.getErrorCodeToErrorCount().get(LengthValidator.ERROR_CODE));
        assertEquals("2", report.getErrorCodeToErrorCount().get(ForbiddenWordValidator.ERROR_CODE));
        assertEquals(2, report.getErrorCodeToErrorCount().size());
    }
    //Verify that when there are support requests and pipelines,
    //the method runs validations and persists results via the repository.
    @Test
    void givenSupportRequestsAndPipelines_whenRunSupportRequestValidation_thenResultsArePersisted() {
        UUID userId = UUID.randomUUID();

        // Mock a single support request
        var supportRequest = new com.dehold.contentmanager.content.customersupport.model.SupportRequest(
                UUID.randomUUID(), userId, "Support request content", null,
                UUID.randomUUID(), Instant.now(), Instant.now());

        UUID id = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        String contentType = "testContentType";
        boolean isValid = true;
        Instant createdAt = Instant.now();

        ValidationResult mockResult = ValidationResult.fromPersistence(id, userId, contentType, contentId,
                isValid, Collections.emptyList(), createdAt);

        // Mock dependencies
        var mockSupportRepo = org.mockito.Mockito.mock(com.dehold.contentmanager.content.customersupport.repository.SupportRequestRepository.class);
        var mockPipeline = org.mockito.Mockito.mock(com.dehold.contentmanager.validation.pipeline.ValidationPipeline.class);
        var mockPipelineFactory = org.mockito.Mockito.mock(com.dehold.contentmanager.validation.pipeline.ValidationPipelineFactory.class);
        var mockResultRepo = org.mockito.Mockito.mock(com.dehold.contentmanager.validation.repository.ValidationResultRepository.class);
        var mockBlogPostService = org.mockito.Mockito.mock(com.dehold.contentmanager.content.blogpost.service.BlogPostService.class);

        org.mockito.Mockito.when(mockSupportRepo.findByUserId(userId)).thenReturn(List.of(supportRequest));
        org.mockito.Mockito.when(mockPipelineFactory.createValidationPipelineForUserAndContentType(userId, "supportrequest"))
                .thenReturn(List.of(mockPipeline));
        org.mockito.Mockito.when(mockPipeline.run(supportRequest)).thenReturn(mockResult);

        var service = new ValidationServiceImpl(mockResultRepo, mockPipelineFactory, mockBlogPostService, mockSupportRepo);

        // Act
        List<ValidationResult> results = service.runSupportRequestValidation(userId);

        // Assert
        assertEquals(1, results.size());
        assertEquals(mockResult, results.get(0));
        verify(mockPipelineFactory, times(1))
                .createValidationPipelineForUserAndContentType(userId, "supportrequest");
        verify(mockResultRepo, times(1)).create(mockResult);
    }

    //Ensure that if the user has no support requests,
    //the method returns an empty result list and no persistence occurs.
    @Test
    void givenNoSupportRequests_whenRunSupportRequestValidation_thenReturnEmptyList() {
        UUID userId = UUID.randomUUID();

        // Mock dependencies
        var mockSupportRepo = org.mockito.Mockito.mock(com.dehold.contentmanager.content.customersupport.repository.SupportRequestRepository.class);
        var mockPipelineFactory = org.mockito.Mockito.mock(com.dehold.contentmanager.validation.pipeline.ValidationPipelineFactory.class);
        var mockResultRepo = org.mockito.Mockito.mock(com.dehold.contentmanager.validation.repository.ValidationResultRepository.class);
        var mockBlogPostService = org.mockito.Mockito.mock(com.dehold.contentmanager.content.blogpost.service.BlogPostService.class);

        org.mockito.Mockito.when(mockSupportRepo.findByUserId(userId)).thenReturn(List.of());

        var service = new ValidationServiceImpl(mockResultRepo, mockPipelineFactory, mockBlogPostService, mockSupportRepo);

        // Act
        List<ValidationResult> results = service.runSupportRequestValidation(userId);

        // Assert
        assertTrue(results.isEmpty());
        verify(mockResultRepo, never()).create(any());
        verify(mockPipelineFactory, never()).createValidationPipelineForUserAndContentType(any(), any());
    }

    //When there are support requests but no pipelines,
    //the service should skip validation and not persist any results.
    @Test
    void givenSupportRequestWithoutPipelines_whenRunSupportRequestValidation_thenNoValidationIsPerformed() {
        UUID userId = UUID.randomUUID();

        var supportRequest = new com.dehold.contentmanager.content.customersupport.model.SupportRequest(
                UUID.randomUUID(), userId, "Support text", null,
                UUID.randomUUID(), Instant.now(), Instant.now());

        var mockSupportRepo = org.mockito.Mockito.mock(com.dehold.contentmanager.content.customersupport.repository.SupportRequestRepository.class);
        var mockPipelineFactory = org.mockito.Mockito.mock(com.dehold.contentmanager.validation.pipeline.ValidationPipelineFactory.class);
        var mockResultRepo = org.mockito.Mockito.mock(com.dehold.contentmanager.validation.repository.ValidationResultRepository.class);
        var mockBlogPostService = org.mockito.Mockito.mock(com.dehold.contentmanager.content.blogpost.service.BlogPostService.class);

        org.mockito.Mockito.when(mockSupportRepo.findByUserId(userId)).thenReturn(List.of(supportRequest));
        org.mockito.Mockito.when(mockPipelineFactory.createValidationPipelineForUserAndContentType(userId, "supportrequest"))
                .thenReturn(List.of()); // no pipelines

        var service = new ValidationServiceImpl(mockResultRepo, mockPipelineFactory, mockBlogPostService, mockSupportRepo);

        // Act
        List<ValidationResult> results = service.runSupportRequestValidation(userId);

        // Assert
        assertTrue(results.isEmpty());
        verify(mockResultRepo, never()).create(any());
    }

    //Test that multiple support requests with multiple pipelines result in multiple persisted results.
    @Test
    void givenMultipleSupportRequestsAndPipelines_whenRunSupportRequestValidation_thenAllResultsPersisted() {
        UUID userId = UUID.randomUUID();

        var req1 = new com.dehold.contentmanager.content.customersupport.model.SupportRequest(
                UUID.randomUUID(), userId, "Support 1", null,
                UUID.randomUUID(), Instant.now(), Instant.now());
        var req2 = new com.dehold.contentmanager.content.customersupport.model.SupportRequest(
                UUID.randomUUID(), userId, "Support 2", null,
                UUID.randomUUID(), Instant.now(), Instant.now());

        var mockPipeline1 = org.mockito.Mockito.mock(com.dehold.contentmanager.validation.pipeline.ValidationPipeline.class);
        var mockPipeline2 = org.mockito.Mockito.mock(com.dehold.contentmanager.validation.pipeline.ValidationPipeline.class);

        UUID contentId = UUID.randomUUID();
        String contentType = "testContentType";
        boolean isValid = true;
        Instant createdAt = Instant.now();

        ValidationResult mockResult = ValidationResult.fromPersistence(UUID.randomUUID(), userId, contentType, contentId,
                true, Collections.emptyList(), createdAt);

        var result1 = ValidationResult.fromPersistence(UUID.randomUUID(), userId, contentType, contentId,
                true, Collections.emptyList(), createdAt);
        var result2 = ValidationResult.fromPersistence(UUID.randomUUID(), userId, contentType, contentId,
                false, List.of(new ValidationError("401", "error")), createdAt);

        org.mockito.Mockito.when(mockPipeline1.run(any())).thenReturn(result1);
        org.mockito.Mockito.when(mockPipeline2.run(any())).thenReturn(result2);

        var mockSupportRepo = org.mockito.Mockito.mock(com.dehold.contentmanager.content.customersupport.repository.SupportRequestRepository.class);
        var mockPipelineFactory = org.mockito.Mockito.mock(com.dehold.contentmanager.validation.pipeline.ValidationPipelineFactory.class);
        var mockResultRepo = org.mockito.Mockito.mock(com.dehold.contentmanager.validation.repository.ValidationResultRepository.class);
        var mockBlogPostService = org.mockito.Mockito.mock(com.dehold.contentmanager.content.blogpost.service.BlogPostService.class);

        org.mockito.Mockito.when(mockSupportRepo.findByUserId(userId)).thenReturn(List.of(req1, req2));
        org.mockito.Mockito.when(mockPipelineFactory.createValidationPipelineForUserAndContentType(userId, "supportrequest"))
                .thenReturn(List.of(mockPipeline1, mockPipeline2));

        var service = new ValidationServiceImpl(mockResultRepo, mockPipelineFactory, mockBlogPostService, mockSupportRepo);

        // Act
        List<ValidationResult> results = service.runSupportRequestValidation(userId);

        // Assert
        assertEquals(4, results.size()); // 2 requests × 2 pipelines
        verify(mockResultRepo, times(4)).create(any(ValidationResult.class));
    }


}
