package com.dehold.contentmanager.validation.service;

import com.dehold.contentmanager.content.blogpost.model.BlogPost;
import com.dehold.contentmanager.content.blogpost.service.BlogPostService;
import com.dehold.contentmanager.validation.model.ValidationResult;
import com.dehold.contentmanager.validation.pipeline.ValidationPipelineFactory;
import com.dehold.contentmanager.validation.repository.ValidationResultRepository;
import com.dehold.contentmanager.validation.web.dto.BlogPostValidationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ValidationServiceTest {

    @Mock
    private ValidationResultRepository repository;

    @Mock
    private ValidationPipelineFactory validationPipelineFactory;

    @Mock
    private BlogPostService blogPostService;

    @Mock
    private ForbiddenWordsService forbiddenWordsService;

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
    void givenTitleBelowMinLength_whenServiceValidates_thenValidationFails() {
        UUID blogPostId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        BlogPost blogPost = new BlogPost(blogPostId, "ABC", "This is valid content for the blog post.",
                Instant.now(), Instant.now(), userId);
        BlogPostValidationRequest request = new BlogPostValidationRequest(10, 100, 20, 500, blogPost);

        validationService.validateBlogPost(request);

        ArgumentCaptor<ValidationResult> captor = ArgumentCaptor.forClass(ValidationResult.class);
        verify(repository, times(1)).create(captor.capture());

        ValidationResult result = captor.getValue();
        assertNotNull(result);
        assertFalse(result.isValid());
        assertFalse(result.getErrors().isEmpty());
        assertEquals(1, result.getErrors().size());
    }

    @Test
    void givenTitleAboveMaxLength_whenServiceValidates_thenValidationFails() {
        UUID blogPostId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String longTitle = "A".repeat(101);
        BlogPost blogPost = new BlogPost(blogPostId, longTitle, "This is valid content for the blog post.",
                Instant.now(), Instant.now(), userId);
        BlogPostValidationRequest request = new BlogPostValidationRequest(5, 100, 20, 500, blogPost);

        validationService.validateBlogPost(request);

        ArgumentCaptor<ValidationResult> captor = ArgumentCaptor.forClass(ValidationResult.class);
        verify(repository, times(1)).create(captor.capture());

        ValidationResult result = captor.getValue();
        assertNotNull(result);
        assertFalse(result.isValid());
        assertFalse(result.getErrors().isEmpty());
    }

    @Test
    void givenContentBelowMinLength_whenServiceValidates_thenValidationFails() {
        UUID blogPostId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        BlogPost blogPost = new BlogPost(blogPostId, "Valid Title", "short",
                Instant.now(), Instant.now(), userId);
        BlogPostValidationRequest request = new BlogPostValidationRequest(5, 100, 50, 500, blogPost);

        validationService.validateBlogPost(request);

        ArgumentCaptor<ValidationResult> captor = ArgumentCaptor.forClass(ValidationResult.class);
        verify(repository, times(1)).create(captor.capture());

        ValidationResult result = captor.getValue();
        assertNotNull(result);
        assertFalse(result.isValid());
        assertFalse(result.getErrors().isEmpty());
    }

    @Test
    void givenContentAboveMaxLength_whenServiceValidates_thenValidationFails() {
        UUID blogPostId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String longContent = "A".repeat(501);
        BlogPost blogPost = new BlogPost(blogPostId, "Valid Title", longContent,
                Instant.now(), Instant.now(), userId);
        BlogPostValidationRequest request = new BlogPostValidationRequest(5, 100, 20, 500, blogPost);

        validationService.validateBlogPost(request);

        ArgumentCaptor<ValidationResult> captor = ArgumentCaptor.forClass(ValidationResult.class);
        verify(repository, times(1)).create(captor.capture());

        ValidationResult result = captor.getValue();
        assertNotNull(result);
        assertFalse(result.isValid());
    }

    @Test
    void givenBothTitleAndContentInvalid_whenServiceValidates_thenValidationFails() {
        UUID blogPostId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String shortTitle = "AB";
        String shortContent = "short";
        BlogPost blogPost = new BlogPost(blogPostId, shortTitle, shortContent,
                Instant.now(), Instant.now(), userId);
        BlogPostValidationRequest request = new BlogPostValidationRequest(5, 100, 50, 500, blogPost);

        validationService.validateBlogPost(request);

        ArgumentCaptor<ValidationResult> captor = ArgumentCaptor.forClass(ValidationResult.class);
        verify(repository, times(1)).create(captor.capture());

        ValidationResult result = captor.getValue();
        assertNotNull(result);
        assertFalse(result.isValid());
    }

    @Test
    void givenForbiddenWordsServiceIsInjected_whenServiceValidates_thenNoNullPointerExceptionOccurs() {
        UUID blogPostId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        BlogPost blogPost = new BlogPost(blogPostId, "Valid Title", "This is valid content for the blog post.",
                Instant.now(), Instant.now(), userId);
        BlogPostValidationRequest request = new BlogPostValidationRequest(5, 100, 20, 500, blogPost);

        // Should not throw NullPointerException
        assertDoesNotThrow(() -> validationService.validateBlogPost(request));

        verify(repository, times(1)).create(any(ValidationResult.class));
    }

    @Test
    void givenValidBlogPostAtBoundary_whenServiceValidates_thenValidationPasses() {
        UUID blogPostId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String titleAtMin = "ABCDE"; // Exactly 5 chars
        String contentAtMin = "A".repeat(20); // Exactly 20 chars
        BlogPost blogPost = new BlogPost(blogPostId, titleAtMin, contentAtMin,
                Instant.now(), Instant.now(), userId);
        BlogPostValidationRequest request = new BlogPostValidationRequest(5, 100, 20, 500, blogPost);

        validationService.validateBlogPost(request);

        ArgumentCaptor<ValidationResult> captor = ArgumentCaptor.forClass(ValidationResult.class);
        verify(repository, times(1)).create(captor.capture());

        ValidationResult result = captor.getValue();
        assertNotNull(result);
        assertTrue(result.isValid());
    }

    @Test
    void givenValidBlogPostAtMaxBoundary_whenServiceValidates_thenValidationPasses() {
        UUID blogPostId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String titleAtMax = "A".repeat(100); // Exactly 100 chars
        String contentAtMax = "A".repeat(500); // Exactly 500 chars
        BlogPost blogPost = new BlogPost(blogPostId, titleAtMax, contentAtMax,
                Instant.now(), Instant.now(), userId);
        BlogPostValidationRequest request = new BlogPostValidationRequest(5, 100, 20, 500, blogPost);

        validationService.validateBlogPost(request);

        ArgumentCaptor<ValidationResult> captor = ArgumentCaptor.forClass(ValidationResult.class);
        verify(repository, times(1)).create(captor.capture());

        ValidationResult result = captor.getValue();
        assertNotNull(result);
        assertTrue(result.isValid());
    }

    @Test
    void givenNullBlogPost_whenServiceValidates_thenThrowsNullPointerException() {
        BlogPostValidationRequest request = new BlogPostValidationRequest(5, 100, 20, 500, null);

        assertThrows(NullPointerException.class, () -> validationService.validateBlogPost(request));
    }

    @Test
    void givenValidationResultIsCreated_whenRepositoryIsVerified_thenResultContainsCorrectUserId() {
        UUID blogPostId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        BlogPost blogPost = new BlogPost(blogPostId, "Valid Title", "This is valid content for the blog post.",
                Instant.now(), Instant.now(), userId);
        BlogPostValidationRequest request = new BlogPostValidationRequest(5, 100, 20, 500, blogPost);

        validationService.validateBlogPost(request);

        ArgumentCaptor<ValidationResult> captor = ArgumentCaptor.forClass(ValidationResult.class);
        verify(repository, times(1)).create(captor.capture());

        ValidationResult result = captor.getValue();
        assertEquals(userId, result.getUserId());
    }

}