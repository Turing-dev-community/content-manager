package com.dehold.contentmanager.validation.service;

import com.dehold.contentmanager.content.blogpost.model.BlogPost;
import com.dehold.contentmanager.content.blogpost.service.BlogPostService;
import com.dehold.contentmanager.content.customersupport.model.SupportRequest;
import com.dehold.contentmanager.content.customersupport.repository.SupportRequestRepository;
import com.dehold.contentmanager.content.customersupport.repository.SupportResponseRepository;
import com.dehold.contentmanager.content.customersupport.service.SupportResponseService;
import com.dehold.contentmanager.content.generic.service.GenericContentService;
import com.dehold.contentmanager.validation.model.ValidationError;
import com.dehold.contentmanager.validation.model.ValidationResult;
import com.dehold.contentmanager.validation.pipeline.ValidationPipeline;
import com.dehold.contentmanager.validation.pipeline.ValidationPipelineFactory;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ValidationServiceTest {

    @Mock
    private ValidationResultRepository repository;

    @InjectMocks
    private ValidationServiceImpl validationService;

    private ValidationPipelineFactory pipelineFactory;
    private BlogPostService blogPostService;
    private SupportRequestRepository supportRepo;
    private SupportResponseService supportResponseService;
    private GenericContentService genericContentService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        pipelineFactory = mock(ValidationPipelineFactory.class);
        blogPostService = mock(BlogPostService.class);
        supportRepo = mock(SupportRequestRepository.class);
        supportResponseService = mock(SupportResponseService.class);
        genericContentService = mock(GenericContentService.class);

        validationService = new ValidationServiceImpl(
                repository,
                pipelineFactory,
                blogPostService,
                supportResponseService,
                supportRepo,
                genericContentService
        );
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
    void givenSupportRequestAndPipeline_whenValidate_thenPersistResult() {

        UUID userId = UUID.randomUUID();

        SupportRequest req = new SupportRequest(
                UUID.randomUUID(), userId,
                "Message", null,
                UUID.randomUUID(), Instant.now(), Instant.now()
        );

        when(supportRepo.findByUserId(userId)).thenReturn(List.of(req));

        // mock pipeline
        ValidationPipeline<SupportRequest> pipeline = mock(ValidationPipeline.class);

        ValidationResult result = ValidationResult.fromPersistence(
                UUID.randomUUID(), userId, "supportrequest",
                req.getId(), true, List.of(), Instant.now()
        );

        when(pipelineFactory.createValidationPipelineForUserAndContentType(userId, "supportrequest"))
                .thenReturn((List) List.of(pipeline));

        when(pipeline.run(req)).thenReturn(result);

        // ACT
        List<ValidationResult> results = validationService.runSupportRequestValidation(userId);

        // ASSERT
        assertEquals(1, results.size());
        assertEquals(result, results.get(0));

        verify(repository, times(1)).create(result);
        verify(pipelineFactory, times(1))
                .createValidationPipelineForUserAndContentType(userId, "supportrequest");
    }

    // ---------------------------------------------------------
    // Multiple support requests × multiple pipelines → ALL results persisted
    // ---------------------------------------------------------
    @Test
    void givenMultipleRequestsAndPipelines_whenValidate_thenAllPersisted() {

        UUID userId = UUID.randomUUID();

        SupportRequest req1 = new SupportRequest(
                UUID.randomUUID(), userId, "Msg1",
                null, UUID.randomUUID(), Instant.now(), Instant.now()
        );
        SupportRequest req2 = new SupportRequest(
                UUID.randomUUID(), userId, "Msg2",
                null, UUID.randomUUID(), Instant.now(), Instant.now()
        );

        when(supportRepo.findByUserId(userId)).thenReturn(List.of(req1, req2));

        ValidationPipeline<SupportRequest> p1 = mock(ValidationPipeline.class);
        ValidationPipeline<SupportRequest> p2 = mock(ValidationPipeline.class);

        // results
        ValidationResult res11 = ValidationResult.fromPersistence(
                UUID.randomUUID(), userId, "supportrequest",
                req1.getId(), true, List.of(), Instant.now()
        );
        ValidationResult res12 = ValidationResult.fromPersistence(
                UUID.randomUUID(), userId, "supportrequest",
                req1.getId(), false, List.of(new ValidationError("101", "Issue")), Instant.now()
        );
        ValidationResult res21 = ValidationResult.fromPersistence(
                UUID.randomUUID(), userId, "supportrequest",
                req2.getId(), true, List.of(), Instant.now()
        );
        ValidationResult res22 = ValidationResult.fromPersistence(
                UUID.randomUUID(), userId, "supportrequest",
                req2.getId(), false, List.of(new ValidationError("202", "Error")), Instant.now()
        );

        when(pipelineFactory.createValidationPipelineForUserAndContentType(userId, "supportrequest"))
                .thenReturn((List) List.of(p1, p2));

        when(p1.run(req1)).thenReturn(res11);
        when(p1.run(req2)).thenReturn(res21);

        when(p2.run(req1)).thenReturn(res12);
        when(p2.run(req2)).thenReturn(res22);

        // ACT
        List<ValidationResult> results = validationService.runSupportRequestValidation(userId);

        // ASSERT
        assertEquals(4, results.size());

        verify(repository, times(4)).create(any(ValidationResult.class));
    }
}
