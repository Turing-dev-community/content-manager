package com.dehold.contentmanager.validation.web;


import com.dehold.contentmanager.content.blogpost.model.BlogPost;
import com.dehold.contentmanager.validation.model.ValidationError;
import com.dehold.contentmanager.validation.model.ValidationPipelineModel;
import com.dehold.contentmanager.validation.model.ValidationResult;
import com.dehold.contentmanager.validation.model.ValidationStepType;
import com.dehold.contentmanager.validation.step.LengthValidator;
import com.dehold.contentmanager.validation.web.dto.BlogPostValidationRequest;
import com.dehold.contentmanager.validation.web.dto.ValidationPipelineCreateDto;
import com.dehold.contentmanager.validation.web.dto.ValidationResponse;
import com.dehold.contentmanager.validation.web.dto.ValidationResultDto;
import com.dehold.contentmanager.validation.web.dto.ValidationStepDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ValidationControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void givenValidBlogPost_whenValidate_thenReturnsSuccess() {
        BlogPost blogPost = new BlogPost(UUID.randomUUID(), "Valid Title", "This is valid content for the blog post."
                , Instant.now(), Instant.now(), UUID.randomUUID());
        BlogPostValidationRequest request = new BlogPostValidationRequest(3, 100, 10, 1000, blogPost);

        var response = restTemplate.postForEntity("http://localhost:" + port + "/api/validate/blogpost", request,
                ValidationResponse.class);

        assertEquals(200, response.getStatusCode().value());
        ValidationResponse expected = new ValidationResponse(BlogPost.class.getSimpleName(),
                ValidationResultDto.from(ValidationResult.valid(blogPost.getClass().getSimpleName(), blogPost.getId()
                        , blogPost.getUserId())));
        ValidationResponse actual = response.getBody();
        assertNotNull(actual);
        assertEquals(expected.getContentType(), actual.getContentType());
        assertEquals(expected.getValidationResult().isValid(), actual.getValidationResult().isValid());
    }

    @Test
    void givenBlogPostWithInvalidTitle_whenValidate_thenReturnsValidationError() {
        BlogPost blogPost = new BlogPost(UUID.randomUUID(), "Not a Valid Title: Is too short", "This is valid content" +
                " for the blog " +
                "post."
                , Instant.now(), Instant.now(), UUID.randomUUID());
        BlogPostValidationRequest request = new BlogPostValidationRequest(50, 100, 10, 1000, blogPost);

        var response = restTemplate.postForEntity("http://localhost:" + port + "/api/validate/blogpost", request,
                ValidationResponse.class);

        assertEquals(200, response.getStatusCode().value());
        var validationErrors = List.of(
                new ValidationError(LengthValidator.ERROR_CODE,
                        LengthValidator.errorMessageTooShort("title")));
        ValidationResponse expected = new ValidationResponse(BlogPost.class.getSimpleName(),
                ValidationResultDto.from(ValidationResult.invalid(blogPost.getClass().getSimpleName(),
                        blogPost.getId(), blogPost.getUserId(), validationErrors)));
        ValidationResponse actual = response.getBody();
        assertNotNull(actual);
        assertEquals(expected.getContentType(), actual.getContentType());
        assertEquals(1, actual.getValidationResult().getErrors().size());
        ValidationError actualError = actual.getValidationResult().getErrors().get(0);
        assertEquals(actualError.code(), validationErrors.getFirst().code());
        assertEquals(actualError.message(), validationErrors.getFirst().message());
    }

    @Test
    void givenBlogPostWithInvalidContent_whenValidate_thenReturnsValidationError() {
        BlogPost blogPost = new BlogPost(UUID.randomUUID(), "Valid Title", "This content is not valid: Too short", Instant.now(), Instant.now(), UUID.randomUUID());
        BlogPostValidationRequest request = new BlogPostValidationRequest(2, 100, 300, 1000, blogPost);

        var response = restTemplate.postForEntity("http://localhost:" + port + "/api/validate/blogpost", request,
                ValidationResponse.class);

        assertEquals(200, response.getStatusCode().value());
        var validationErrors = List.of(
                new ValidationError(LengthValidator.ERROR_CODE,
                        LengthValidator.errorMessageTooShort("content")));
        ValidationResponse expected = new ValidationResponse(BlogPost.class.getSimpleName(),
                ValidationResultDto.from(ValidationResult.invalid(blogPost.getClass().getSimpleName(),
                        blogPost.getId(),  blogPost.getUserId(),
                        validationErrors)));
        ValidationResponse actual = response.getBody();
        assertNotNull(actual);
        assertEquals(expected.getContentType(), actual.getContentType());
        assertEquals(1, actual.getValidationResult().getErrors().size());
        ValidationError actualError = actual.getValidationResult().getErrors().get(0);
        assertEquals(actualError.code(), validationErrors.getFirst().code());
        assertEquals(actualError.message(), validationErrors.getFirst().message());
    }

    @Test
    void givenBlogPostWithInvalidField_whenValidate_thenReturnsValidationError() {
        BlogPost blogPost = new BlogPost(UUID.randomUUID(), "Valid Title", "This content is not valid: Too short", Instant.now(), Instant.now(), UUID.randomUUID());
        BlogPostValidationRequest request = new BlogPostValidationRequest(2, 100, 300, 1000, blogPost);

        var response = restTemplate.postForEntity("http://localhost:" + port + "/api/validate/blogpost", request,
                ValidationResponse.class);

        assertEquals(200, response.getStatusCode().value());
        var validationErrors = List.of(
                new ValidationError(LengthValidator.ERROR_CODE,
                        LengthValidator.errorMessageTooShort("content")));
        ValidationResponse expected = new ValidationResponse(BlogPost.class.getSimpleName(),
                ValidationResultDto.from(ValidationResult.invalid(blogPost.getClass().getSimpleName(),
                        blogPost.getId(), blogPost.getUserId(), validationErrors)));
        ValidationResponse actual = response.getBody();
        assertNotNull(actual);
        assertEquals(expected, actual);
    }

    @Test
    void givenValidContent_whenValidate_thenResponseContainsValidationResultIncludingContentTypeAndContentId() {
        BlogPost blogPost = new BlogPost(UUID.randomUUID(), "Valid Title", "This is valid content for the blog post."
                , Instant.now(), Instant.now(), UUID.randomUUID());
        BlogPostValidationRequest request = new BlogPostValidationRequest(3, 100, 10, 1000, blogPost);

        var response = restTemplate.postForEntity("http://localhost:" + port + "/api/validate/blogpost", request,
                ValidationResponse.class);

        assertEquals(200, response.getStatusCode().value());
        ValidationResponse expected = new ValidationResponse(BlogPost.class.getSimpleName(),
                ValidationResultDto.from(ValidationResult.valid(blogPost.getClass().getSimpleName(), blogPost.getId(), blogPost.getUserId())));
        ValidationResponse actual = response.getBody();
        assertNotNull(actual);
        assertEquals(expected.getValidationResult().getContentType(), actual.getValidationResult().getContentType());
        assertEquals(expected.getValidationResult().getContentId(), actual.getValidationResult().getContentId());
    }

    @Test
    void givenInvalidContent_whenValidate_thenResponseContainsValidationResultIncludingContentTypeAndContentId() {
        BlogPost blogPost = new BlogPost(UUID.randomUUID(), "Invalid: This title is too short", "This is valid " +
                "content for the " +
                "blog post."
                , Instant.now(), Instant.now(), UUID.randomUUID());
        BlogPostValidationRequest request = new BlogPostValidationRequest(50, 100, 10, 1000, blogPost);

        var response = restTemplate.postForEntity("http://localhost:" + port + "/api/validate/blogpost", request,
                ValidationResponse.class);

        assertEquals(200, response.getStatusCode().value());
        ValidationResponse expected = new ValidationResponse(BlogPost.class.getSimpleName(),
                ValidationResultDto.from(ValidationResult.valid(blogPost.getClass().getSimpleName(), blogPost.getId(), blogPost.getUserId())));
        ValidationResponse actual = response.getBody();
        assertNotNull(actual);
        assertEquals(expected.getValidationResult().getContentType(), actual.getValidationResult().getContentType());
        assertEquals(expected.getValidationResult().getContentId(), actual.getValidationResult().getContentId());
    }

    @Test
    void givenOneBlogPostAndValidationPipeline_whenRequestValidationRun_thenReturnValidationResult() {
        UUID userId = UUID.randomUUID();
        var createBlogPostResponse = restTemplate.postForEntity("http://localhost:" + port + "/api/blogposts",
                Map.of("title", "Test Blog Post Title", "content", "This is test content for the blog post", "userId", userId),
                BlogPost.class);
        assertEquals(201, createBlogPostResponse.getStatusCode().value());
        assertNotNull(createBlogPostResponse.getBody());

        var createPipelineDto = new ValidationPipelineCreateDto();
        createPipelineDto.setUserId(userId);
        createPipelineDto.setContentType("blogpost");
        createPipelineDto.setDescription("Test pipeline for blog post validation");
        createPipelineDto.setSteps(List.of(
                new ValidationStepDto(null, ValidationStepType.LENGTH_VALIDATION, "title",
                        Map.of("minLength", "5", "maxLength", "100"), true),
                new ValidationStepDto(null, ValidationStepType.LENGTH_VALIDATION, "content",
                        Map.of("minLength", "10", "maxLength", "1000"), true)
        ));

        var createPipelineResponse = restTemplate.postForEntity("http://localhost:" + port + "/api/validation-pipelines",
                createPipelineDto, ValidationPipelineModel.class);
        assertEquals(201, createPipelineResponse.getStatusCode().value());
        assertNotNull(createPipelineResponse.getBody());

        var response = restTemplate.postForEntity("http://localhost:" + port + "/api/validate/validate-blogposts?userId=" + userId,
                null, ValidationResult[].class);

        assertEquals(200, response.getStatusCode().value());
        var results = response.getBody();
        assertNotNull(results);
        assertEquals(1, results.length);

        ValidationResult result = results[0];
        assertEquals("BlogPost", result.getContentType());
        assertEquals(userId, result.getUserId());
        assertTrue(result.isValid());
    }

}