package com.dehold.contentmanager.validation.web;


import com.dehold.contentmanager.content.blogpost.model.BlogPost;
import com.dehold.contentmanager.exception.CustomErrorResponse;
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

import static org.junit.jupiter.api.Assertions.*;

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
        BlogPost blogPost = new BlogPost(UUID.randomUUID(), "Test Blog Post Title", "This is test content for the blog post", Instant.now(), Instant.now(), userId);

        var createBlogPostResponse = restTemplate.postForEntity("http://localhost:" + port + "/api/blogposts",
                blogPost,
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
                null, ValidationResponse[].class);

        assertEquals(200, response.getStatusCode().value());
        var results = response.getBody();
        assertNotNull(results);
        assertEquals(1, results.length);

        ValidationResponse validationResponse = results[0];
        assertEquals("BlogPost", validationResponse.getContentType());
        ValidationResultDto result = validationResponse.getValidationResult();
        assertEquals("BlogPost", result.getContentType());
        assertEquals(userId, result.getUserId());
        assertTrue(result.isValid());
    }

    @Test
    void givenInvalidBlogPostAndValidationPipeline_whenRequestValidationRun_thenReturnValidationResultWithErrors() {
        UUID userId = UUID.randomUUID();
        BlogPost blogPost = new BlogPost(UUID.randomUUID(), "Shrt", "Too short", Instant.now(), Instant.now(), userId);

        var createBlogPostResponse = restTemplate.postForEntity("http://localhost:" + port + "/api/blogposts",
                blogPost,
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
                null, ValidationResponse[].class);

        assertEquals(200, response.getStatusCode().value());
        var results = response.getBody();
        assertNotNull(results);
        assertEquals(1, results.length);

        ValidationResponse validationResponse = results[0];
        assertEquals("BlogPost", validationResponse.getContentType());
        ValidationResultDto result = validationResponse.getValidationResult();
        assertEquals("BlogPost", result.getContentType());
        assertEquals(userId, result.getUserId());
        assertFalse(result.isValid());
        assertEquals(2, result.getErrors().size());
        assertTrue(result.getErrors().stream().anyMatch(
                e -> e.code().equals(LengthValidator.ERROR_CODE) &&
                        e.message().equals(LengthValidator.errorMessageTooShort("title"))
        ));
        assertTrue(result.getErrors().stream().anyMatch(
                e -> e.code().equals(LengthValidator.ERROR_CODE) &&
                        e.message().equals(LengthValidator.errorMessageTooShort("content"))
        ));
    }

    @Test
    void givenMultipleBlogPostsWithViolations_whenValidateBlogPosts_thenReturnsAllValidationResults() {
        UUID userId = UUID.randomUUID();

        // Violation in content
        BlogPost blogPost1 = new BlogPost(UUID.randomUUID(), "Hi", "This is valid content for the first blog post", Instant.now(), Instant.now(), userId);
        var createBlogPost1Response = restTemplate.postForEntity("http://localhost:" + port + "/api/blogposts",
                blogPost1, BlogPost.class);
        assertEquals(201, createBlogPost1Response.getStatusCode().value());
        assertNotNull(createBlogPost1Response.getBody());

        //Violation in title
        BlogPost blogPost2 = new BlogPost(UUID.randomUUID(), "Valid Title Here", "Short", Instant.now(), Instant.now(), userId);
        var createBlogPost2Response = restTemplate.postForEntity("http://localhost:" + port + "/api/blogposts",
                blogPost2, BlogPost.class);
        assertEquals(201, createBlogPost2Response.getStatusCode().value());
        assertNotNull(createBlogPost2Response.getBody());

        //Vioalation in both content and title
        BlogPost blogPost3 = new BlogPost(UUID.randomUUID(), "Bad", "Bad", Instant.now(), Instant.now(), userId);
        var createBlogPost3Response = restTemplate.postForEntity("http://localhost:" + port + "/api/blogposts",
                blogPost3, BlogPost.class);
        assertEquals(201, createBlogPost3Response.getStatusCode().value());
        assertNotNull(createBlogPost3Response.getBody());

        var createPipelineDto = new ValidationPipelineCreateDto();
        createPipelineDto.setUserId(userId);
        createPipelineDto.setContentType("blogpost");
        createPipelineDto.setDescription("Test pipeline for multiple blog post validation");
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
                null, ValidationResponse[].class);

        assertEquals(200, response.getStatusCode().value());
        var results = response.getBody();
        assertNotNull(results);
        assertEquals(3, results.length);

        for (ValidationResponse validationResponse : results) {
            assertEquals("BlogPost", validationResponse.getContentType());
            ValidationResultDto result = validationResponse.getValidationResult();
            assertEquals("BlogPost", result.getContentType());
            assertEquals(userId, result.getUserId());
            assertFalse(result.isValid()); // All should be invalid
        }

        int titleErrors = 0;
        int contentErrors = 0;
        int bothErrors = 0;

        for (ValidationResponse validationResponse : results) {
            ValidationResultDto result = validationResponse.getValidationResult();
            boolean hasTitleError = result.getErrors().stream().anyMatch(
                    e -> e.code().equals(LengthValidator.ERROR_CODE) &&
                            e.message().equals(LengthValidator.errorMessageTooShort("title"))
            );
            boolean hasContentError = result.getErrors().stream().anyMatch(
                    e -> e.code().equals(LengthValidator.ERROR_CODE) &&
                            e.message().equals(LengthValidator.errorMessageTooShort("content"))
            );

            if (hasTitleError && hasContentError) {
                bothErrors++;
                assertEquals(2, result.getErrors().size());
            } else if (hasTitleError) {
                titleErrors++;
                assertEquals(1, result.getErrors().size());
            } else if (hasContentError) {
                contentErrors++;
                assertEquals(1, result.getErrors().size());
            }
        }

        assertEquals(1, titleErrors);
        assertEquals(1, contentErrors);
        assertEquals(1, bothErrors);
    }

}