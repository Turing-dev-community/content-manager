package com.dehold.contentmanager.validation.service;

import com.dehold.contentmanager.content.blogpost.model.BlogPost;
import com.dehold.contentmanager.validation.model.ValidationResult;
import com.dehold.contentmanager.validation.repository.ValidationResultRepository;
import com.dehold.contentmanager.validation.web.dto.BlogPostValidationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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

}