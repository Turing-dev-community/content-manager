package com.dehold.contentmanager.validation.service;

import com.dehold.contentmanager.content.blogpost.model.BlogPost;
import com.dehold.contentmanager.content.blogpost.service.BlogPostService;
import com.dehold.contentmanager.content.customersupport.model.SupportRequest;
import com.dehold.contentmanager.content.customersupport.repository.SupportRequestRepository;
import com.dehold.contentmanager.content.customersupport.repository.SupportResponseRepository;
import com.dehold.contentmanager.content.customersupport.service.SupportResponseService;
import com.dehold.contentmanager.content.generic.service.GenericContentService;
import com.dehold.contentmanager.exception.EntityNotFoundException;
import com.dehold.contentmanager.user.repository.UserRepository;
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
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        pipelineFactory = mock(ValidationPipelineFactory.class);
        blogPostService = mock(BlogPostService.class);
        supportRepo = mock(SupportRequestRepository.class);
        supportResponseService = mock(SupportResponseService.class);
        genericContentService = mock(GenericContentService.class);
        userRepository = mock(UserRepository.class);

        validationService = new ValidationServiceImpl(
                repository,
                pipelineFactory,
                blogPostService,
                supportResponseService,
                supportRepo,
                genericContentService,
                userRepository
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

        ValidationReportDto report = validationService.generateValidationReport(userId, false);
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

        ValidationReportDto report = validationService.generateValidationReport(userId, false);
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

        ValidationReportDto report = validationService.generateValidationReport(userId, false);
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

        ValidationReportDto report = validationService.generateValidationReport(userId, false);
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

        verify(repository, times(1)).upsert(same(result), any(UUID.class));
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

        verify(repository, times(4)).upsert(any(ValidationResult.class), any());
    }

    //Same runId used for the whole Support Request run
    @Test
    void givenMultipleResults_whenValidateSupportRequests_thenSameRunIdIsUsedForAllUpserts() {

        UUID userId = UUID.randomUUID();
        SupportRequest req = new SupportRequest(
                UUID.randomUUID(), userId,
                "Message", null,
                UUID.randomUUID(), Instant.now(), Instant.now()
        );

        when(supportRepo.findByUserId(userId)).thenReturn(List.of(req));

        ValidationPipeline<SupportRequest> pipeline = mock(ValidationPipeline.class);

        ValidationResult r1 = ValidationResult.fromPersistence(
                UUID.randomUUID(), userId, "supportrequest", req.getId(), true, List.of(), Instant.now()
        );
        ValidationResult r2 = ValidationResult.fromPersistence(
                UUID.randomUUID(), userId, "supportrequest", req.getId(), false, List.of(), Instant.now()
        );

        when(pipelineFactory.createValidationPipelineForUserAndContentType(userId, "supportrequest"))
                .thenReturn((List) List.of(pipeline, pipeline));

        when(pipeline.run(req))
                .thenReturn(r1)
                .thenReturn(r2);

        validationService.runSupportRequestValidation(userId);

        ArgumentCaptor<UUID> runIdCaptor = ArgumentCaptor.forClass(UUID.class);

        verify(repository, times(2)).upsert(any(ValidationResult.class), runIdCaptor.capture());

        List<UUID> allRunIds = runIdCaptor.getAllValues();
        assertEquals(2, allRunIds.size());

        // Both calls must have same runId
        assertEquals(allRunIds.get(0), allRunIds.get(1));
    }


    //runId must change between two different executions
    @Test
    void whenValidateCalledTwice_thenRunIdIsDifferentForEachExecution() {

        UUID userId = UUID.randomUUID();
        SupportRequest req = new SupportRequest(
                UUID.randomUUID(), userId,
                "Message", null,
                UUID.randomUUID(), Instant.now(), Instant.now()
        );

        when(supportRepo.findByUserId(userId)).thenReturn(List.of(req));

        ValidationPipeline<SupportRequest> pipeline = mock(ValidationPipeline.class);

        ValidationResult vr = ValidationResult.fromPersistence(
                UUID.randomUUID(), userId, "supportrequest",
                req.getId(), true, List.of(), Instant.now()
        );

        when(pipelineFactory.createValidationPipelineForUserAndContentType(userId, "supportrequest"))
                .thenReturn((List) List.of(pipeline));

        when(pipeline.run(req)).thenReturn(vr);

        // First run
        validationService.runSupportRequestValidation(userId);
        ArgumentCaptor<UUID> run1 = ArgumentCaptor.forClass(UUID.class);
        verify(repository).upsert(any(), run1.capture());

        reset(repository);

        // Second run
        validationService.runSupportRequestValidation(userId);
        ArgumentCaptor<UUID> run2 = ArgumentCaptor.forClass(UUID.class);
        verify(repository).upsert(any(), run2.capture());

        assertNotEquals(run1.getValue(), run2.getValue());
    }

    //runId is never null
    @Test
    void givenSupportRequest_whenValidate_thenRunIdIsNeverNull() {

        UUID userId = UUID.randomUUID();
        SupportRequest req = new SupportRequest(
                UUID.randomUUID(), userId,
                "Message", null,
                UUID.randomUUID(), Instant.now(), Instant.now()
        );

        when(supportRepo.findByUserId(userId)).thenReturn(List.of(req));

        ValidationPipeline<SupportRequest> pipeline = mock(ValidationPipeline.class);

        ValidationResult vr = ValidationResult.fromPersistence(
                UUID.randomUUID(), userId, "supportrequest",
                req.getId(), true, List.of(), Instant.now()
        );

        when(pipelineFactory.createValidationPipelineForUserAndContentType(userId, "supportrequest"))
                .thenReturn((List) List.of(pipeline));

        when(pipeline.run(req)).thenReturn(vr);

        validationService.runSupportRequestValidation(userId);

        ArgumentCaptor<UUID> runIdCaptor = ArgumentCaptor.forClass(UUID.class);

        verify(repository).upsert(any(ValidationResult.class), runIdCaptor.capture());

        assertNotNull(runIdCaptor.getValue());
    }

    //Service returns duplicates but persists only once per unique key
    @Test
    void givenDuplicatePipelineResults_whenValidateSupportRequest_thenUpsertDoesNotPersistDuplicates() {

        UUID userId = UUID.randomUUID();

        SupportRequest req = new SupportRequest(
                UUID.randomUUID(), userId,
                "Message", null,
                UUID.randomUUID(), Instant.now(), Instant.now()
        );

        when(supportRepo.findByUserId(userId)).thenReturn(List.of(req));

        // mock two pipelines that produce same ValidationResult (duplicate)
        ValidationPipeline<SupportRequest> p1 = mock(ValidationPipeline.class);
        ValidationPipeline<SupportRequest> p2 = mock(ValidationPipeline.class);

        ValidationResult duplicate = ValidationResult.fromPersistence(
                UUID.randomUUID(), userId, "supportrequest",
                req.getId(), true, List.of(), Instant.now()
        );

        when(pipelineFactory.createValidationPipelineForUserAndContentType(userId, "supportrequest"))
                .thenReturn((List) List.of(p1, p2));

        when(p1.run(req)).thenReturn(duplicate);
        when(p2.run(req)).thenReturn(duplicate); // duplicate result

        // run validation
        List<ValidationResult> resultList = validationService.runSupportRequestValidation(userId);

        // service should return both (because two pipelines ran)
        assertEquals(2, resultList.size());
        assertEquals(duplicate, resultList.get(0));
        assertEquals(duplicate, resultList.get(1));

        // capture runId to ensure matching
        ArgumentCaptor<UUID> runIdCaptor = ArgumentCaptor.forClass(UUID.class);

        // should call upsert twice with same runId and same identifiers → DB deduplicates
        verify(repository, times(2)).upsert(eq(duplicate), runIdCaptor.capture());

        List<UUID> runIds = runIdCaptor.getAllValues();
        assertEquals(runIds.get(0), runIds.get(1)); // same run
    }



    // ---------------------------------------------------------
// TESTS FOR validateRestoredBlogPost()
// ---------------------------------------------------------

    @Test
    void givenNonExistentUser_whenValidateRestoredBlogPost_thenThrowEntityNotFound() {

        UUID userId = UUID.randomUUID();
        BlogPost post = new BlogPost(
                UUID.randomUUID(),
                "Title",
                "Content",
                Instant.now(),
                Instant.now(),
                userId
        );

        when(userRepository.getUserById(userId)).thenReturn(java.util.Optional.empty());

        EntityNotFoundException ex = assertThrows(
                EntityNotFoundException.class,
                () -> validationService.validateRestoredBlogPost(post)
        );

        assertEquals("The entity User with id " + userId + " does not exist", ex.getMessage());
        verifyNoInteractions(pipelineFactory);
        verify(repository, never()).create(any());
    }

    @Test
    void givenValidUserAndPipelines_whenValidateRestoredBlogPost_thenPipelinesRunAndResultsPersisted() {

        UUID userId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();

        BlogPost post = new BlogPost(
                postId,
                "Test",
                "Content",
                Instant.now(),
                Instant.now(),
                userId
        );

        when(userRepository.getUserById(userId)).thenReturn(java.util.Optional.of(mock()));

        // mock pipelines
        ValidationPipeline<BlogPost> p1 = mock(ValidationPipeline.class);
        ValidationPipeline<BlogPost> p2 = mock(ValidationPipeline.class);

        ValidationResult r1 = ValidationResult.valid("blogpost", postId, userId);
        ValidationResult r2 = ValidationResult.invalid("blogpost", postId, userId, List.of(
                new ValidationError("101", "BAD CONTENT")
        ));

        when(pipelineFactory.createValidationPipelineForUserAndContentType(userId, "blogpost"))
                .thenReturn((List) List.of(p1, p2));

        when(p1.run(post)).thenReturn(r1);
        when(p2.run(post)).thenReturn(r2);

        // ACT
        List<ValidationResult> results = validationService.validateRestoredBlogPost(post);

        // ASSERT
        assertEquals(2, results.size());
        assertTrue(results.contains(r1));
        assertTrue(results.contains(r2));

        verify(repository, times(2)).create(any(ValidationResult.class));
        verify(pipelineFactory, times(1))
                .createValidationPipelineForUserAndContentType(userId, "blogpost");
    }

    @Test
    void givenUserExistsButNoPipelines_whenValidateRestoredBlogPost_thenReturnEmptyList() {

        UUID userId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();

        BlogPost post = new BlogPost(
                postId,
                "A",
                "B",
                Instant.now(),
                Instant.now(),
                userId
        );

        when(userRepository.getUserById(userId)).thenReturn(java.util.Optional.of(mock()));
        when(pipelineFactory.createValidationPipelineForUserAndContentType(userId, "blogpost"))
                .thenReturn(List.of());

        List<ValidationResult> results = validationService.validateRestoredBlogPost(post);

        assertTrue(results.isEmpty());
        verify(repository, never()).create(any());
    }

}
