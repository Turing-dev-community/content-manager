package com.dehold.contentmanager.user.web;

import com.dehold.contentmanager.ContentManagerApplicationTests;
import com.dehold.contentmanager.content.blogpost.model.BlogPost;
import com.dehold.contentmanager.content.blogpost.repository.BlogPostRepository;
import com.dehold.contentmanager.content.customersupport.model.SupportRequest;
import com.dehold.contentmanager.content.customersupport.repository.SupportRequestRepository;
import com.dehold.contentmanager.exception.CustomErrorResponse;
import com.dehold.contentmanager.user.model.User;
import com.dehold.contentmanager.user.repository.UserRepository;
import com.dehold.contentmanager.user.web.dto.CreateUserRequest;
import com.dehold.contentmanager.user.web.dto.UpdateUserRequest;
import com.dehold.contentmanager.validation.model.ValidationError;
import com.dehold.contentmanager.validation.model.ValidationPipelineModel;
import com.dehold.contentmanager.validation.model.ValidationResult;
import com.dehold.contentmanager.validation.model.ValidationStepType;
import com.dehold.contentmanager.validation.step.LengthValidator;
import com.dehold.contentmanager.validation.step.PhoneNumberForbiddenValidator;
import com.dehold.contentmanager.validation.web.dto.BlogPostValidationRequest;
import com.dehold.contentmanager.validation.web.dto.ValidationPipelineCreateDto;
import com.dehold.contentmanager.validation.web.dto.ValidationReportDto;
import com.dehold.contentmanager.validation.web.dto.ValidationResponse;
import com.dehold.contentmanager.validation.web.dto.ValidationResultDto;
import com.dehold.contentmanager.validation.web.dto.ValidationStepDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dehold.contentmanager.validation.repository.ValidationResultRepository;
import com.dehold.contentmanager.content.customersupport.model.SupportRequest;
import com.dehold.contentmanager.content.customersupport.model.SupportResponse;
import com.dehold.contentmanager.content.customersupport.repository.SupportRequestRepository;
import com.dehold.contentmanager.content.customersupport.repository.SupportResponseRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;


class UserControllerIntegrationTest  extends ContentManagerApplicationTests {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BlogPostRepository blogPostRepository;

    @Autowired
    private ValidationResultRepository validationResultRepository;

    @Autowired
    private SupportRequestRepository supportRequestRepository;

    @Autowired
    private SupportResponseRepository supportResponseRepository;

    private String uniqueUsername() {
        return "TestUser-" + UUID.randomUUID();
    }

    @Test
    void createUser_shouldReturnCreatedUser() {
        CreateUserRequest request = new CreateUserRequest();
        request.setAlias("Integration Test User");
        request.setEmail("integration-" + UUID.randomUUID() + "@example.com");
        request.setUsername(uniqueUsername());
        request.setPassword("TestUser-" + UUID.randomUUID());
        request.isEnabled();
        ResponseEntity<User> response = restTemplate.postForEntity("http://localhost:" + port + "/api/users", request, User.class);
        assertEquals(201, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(request.getAlias(), response.getBody().getAlias());
        assertEquals(request.getEmail(), response.getBody().getEmail());
    }

    @Test
    void getUser_shouldReturnUser() {
        String uniqueEmail = "integration-" + UUID.randomUUID() + "@example.com";
        User user = new User(UUID.randomUUID(), "Integration Test User", uniqueEmail, Instant.now(), Instant.now(), uniqueUsername(), "TestUser-" + UUID.randomUUID(), true);
        userRepository.createUser(user);
        ResponseEntity<User> response = restTemplate.getForEntity("http://localhost:" + port + "/api/users/" + user.getId(), User.class);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(user.getAlias(), response.getBody().getAlias());
        assertEquals(user.getEmail(), response.getBody().getEmail());
    }

    @Test
    void updateUser_shouldReturnUpdatedUser() {
        String oldEmail = "old-" + UUID.randomUUID() + "@example.com";
        String newEmail = "new-" + UUID.randomUUID() + "@example.com";
        User user = new User(UUID.randomUUID(), "Old Name", oldEmail, Instant.now(), Instant.now(), uniqueUsername(), "TestUser-" + UUID.randomUUID(), true);
        userRepository.createUser(user);
        UpdateUserRequest request = new UpdateUserRequest();
        request.setAlias("New Name");
        request.setEmail(newEmail);
        request.setUsername("TestName");
        request.setPassword("TestUser-" + UUID.randomUUID());
        request.isEnabled();
        HttpEntity<UpdateUserRequest> entity = new HttpEntity<>(request);
        ResponseEntity<User> response = restTemplate.exchange("http://localhost:" + port + "/api/users/" + user.getId(), HttpMethod.PUT, entity, User.class);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(request.getAlias(), response.getBody().getAlias());
        assertEquals(request.getEmail(), response.getBody().getEmail());
    }

    @Test
    void getUser_shouldReturnNotFound() {
        UUID nonExistentId = UUID.randomUUID();

        ResponseEntity<CustomErrorResponse> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/api/users/" + nonExistentId,
                CustomErrorResponse.class
        );
        assertNotNull(response.getBody());
        CustomErrorResponse errorResponse = response.getBody();
        assertEquals(404, errorResponse.getHttpStatusCode());
        assertNotNull(response.getBody());
        assertEquals("The entity User with id " + nonExistentId + " does not exist", errorResponse.getError());
    }

    @Test
    void getBlogpostsByUserId_shouldReturnBlogposts() {
        User user = new User(UUID.randomUUID(), "Blogpost User", "blogpostuser-" + UUID.randomUUID() + "@example.com", Instant.now(), Instant.now(), uniqueUsername(), "TestUser-" + UUID.randomUUID(), true);
        userRepository.createUser(user);
        BlogPost blogPost = new BlogPost(UUID.randomUUID(), "Test Blogpost", "This is a test blogpost.", Instant.now(), Instant.now(), user.getId());
        blogPostRepository.createBlogPost(blogPost);

        ResponseEntity<BlogPost[]> response = restTemplate.getForEntity("http://localhost:" + port + "/api/users" +
                "/" + user.getId() + "/blogposts", BlogPost[].class);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().length);
        assertEquals(blogPost.getId(), response.getBody()[0].getId());
    }

    @Test
    void getBlogPostsForNonExistentUser_shouldReturnNotFoundResponseCode() {
        UUID nonExistentUserId = UUID.randomUUID();

        ResponseEntity<CustomErrorResponse> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/api/users/" + nonExistentUserId + "/blogposts",
                CustomErrorResponse.class
        );

        assertEquals(404, response.getStatusCode().value());
        assertNotNull(response.getBody());
        CustomErrorResponse errorResponse = response.getBody();
        assertEquals(404, errorResponse.getHttpStatusCode());
    }

    @Test
    void getBlogPostsForNonExistentUser_shouldReturnNotFoundErrorMessage() {
        UUID nonExistentUserId = UUID.randomUUID();

        ResponseEntity<CustomErrorResponse> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/users/" + nonExistentUserId + "/blogposts",
                CustomErrorResponse.class
        );

        assertEquals(404, response.getStatusCode().value());
        assertNotNull(response.getBody());
        CustomErrorResponse errorResponse = response.getBody();
        assertEquals("The entity User with id " + nonExistentUserId + " does not exist", errorResponse.getError());
    }

    @Test
    void getBlogPostsForNonExistentUser_shouldReturnNotFoundErrorPath() {
        UUID nonExistentUserId = UUID.randomUUID();

        ResponseEntity<CustomErrorResponse> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/users/" + nonExistentUserId + "/blogposts",
                CustomErrorResponse.class
        );

        assertEquals(404, response.getStatusCode().value());
        assertNotNull(response.getBody());
        CustomErrorResponse errorResponse = response.getBody();
        assertTrue(errorResponse.getPath().contains("/api/users/" + nonExistentUserId + "/blogposts"));
    }

    @Test
    void getBlogPostsForExistentUserThatHasNoPosts_shouldReturnEmptyList() {
        User user = new User(UUID.randomUUID(), "Blogpost User", "blogpostuser-" + UUID.randomUUID() + "@example.com", Instant.now(), Instant.now(), uniqueUsername(), "TestUser-" + UUID.randomUUID(), true);
        userRepository.createUser(user);

        ResponseEntity<BlogPost[]> response = restTemplate.getForEntity("http://localhost:" + port + "/api/users" +
                "/" + user.getId() + "/blogposts", BlogPost[].class);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().length);
    }

    @Test
    void givenOneValidationResult_getValidationResultsByUser_shouldReturnOneValidationResult() {
        User user = new User(UUID.randomUUID(), "Validation User", "validationuser-" + UUID.randomUUID() + "@example.com", Instant.now(), Instant.now(), uniqueUsername(), "TestUser-" + UUID.randomUUID(), true);
        userRepository.createUser(user);

        BlogPost blogPost = new BlogPost(UUID.randomUUID(), "Validation Blogpost", "This is a validation blogpost.", Instant.now(), Instant.now(), user.getId());
        blogPostRepository.createBlogPost(blogPost);

        BlogPostValidationRequest request = new BlogPostValidationRequest(3, 100, 10, 1000, blogPost);
        ValidationResponse validationResponse = restTemplate.postForEntity("http://localhost:" + port + "/api" +
                "/validate/blogpost", request, ValidationResponse.class).getBody();
        assertNotNull(validationResponse);

        ResponseEntity<ValidationResultDto[]> response =
                restTemplate.getForEntity("http://localhost:" + port + "/api/users/" +
                user.getId() + "/validation-results", ValidationResultDto[].class);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().length);
    }

    @Test
    void givenMultipleValidationResults_getValidationResultsByUser_shouldReturnMultipleValidationResults() {
        User user = new User(UUID.randomUUID(), "Validation User", "validationuser-" + UUID.randomUUID() + "@example.com", Instant.now(), Instant.now(), uniqueUsername(), "TestUser-" + UUID.randomUUID(), true);
        userRepository.createUser(user);

        BlogPost blogPost = new BlogPost(UUID.randomUUID(), "Validation Blogpost", "This is a validation blogpost.", Instant.now(), Instant.now(), user.getId());
        blogPostRepository.createBlogPost(blogPost);

        BlogPostValidationRequest request = new BlogPostValidationRequest(3, 100, 10, 1000, blogPost);
        // Run validation multiple times to create multiple validation results
        restTemplate.postForEntity("http://localhost:" + port + "/api" +
                "/validate/blogpost", request, ValidationResponse.class);
        restTemplate.postForEntity("http://localhost:" + port + "/api" +
                "/validate/blogpost", request, ValidationResponse.class);
        restTemplate.postForEntity("http://localhost:" + port + "/api" +
                "/validate/blogpost", request, ValidationResponse.class);


        ResponseEntity<ValidationResultDto[]> response =
                restTemplate.getForEntity("http://localhost:" + port + "/api/users/" +
                        user.getId() + "/validation-results", ValidationResultDto[].class);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(3, response.getBody().length);
    }

    @Test
    void givenOneValidationResult_getValidationResultsByUser_shouldReturnRightPayload() {
        User user = new User(UUID.randomUUID(), "Validation User", "validationuser-" + UUID.randomUUID() + "@example.com", Instant.now(), Instant.now(), uniqueUsername(), "TestPassword@123", true);
        userRepository.createUser(user);

        BlogPost blogPost = new BlogPost(UUID.randomUUID(), "Validation Blogpost", "This is a validation blogpost.", Instant.now(), Instant.now(), user.getId());
        blogPostRepository.createBlogPost(blogPost);

        BlogPostValidationRequest request = new BlogPostValidationRequest(3, 100, 10, 1000, blogPost);
        ValidationResponse validationResponse = restTemplate.postForEntity("http://localhost:" + port + "/api" +
                "/validate/blogpost", request, ValidationResponse.class).getBody();
        assertNotNull(validationResponse);


        ResponseEntity<ValidationResultDto[]> response =
                restTemplate.getForEntity("http://localhost:" + port + "/api/users/" +
                        user.getId() + "/validation-results", ValidationResultDto[].class);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());

        ValidationResultDto actual = response.getBody()[0];
        ValidationResultDto expected = validationResponse.getValidationResult();
        assertEquals(expected, actual);
    }

    @Test
    void givenMultipleValidationResults_getValidationResultsByUser_shouldReturnRightPayload() {
        User user = new User(UUID.randomUUID(), "Validation User", "validationuser-" + UUID.randomUUID() + "@example.com", Instant.now(), Instant.now(), uniqueUsername(), "TestUser-" + UUID.randomUUID(), true);
        userRepository.createUser(user);

        BlogPost blogPost = new BlogPost(UUID.randomUUID(), "Validation Blogpost", "This is a validation blogpost.", Instant.now(), Instant.now(), user.getId());
        blogPostRepository.createBlogPost(blogPost);

        BlogPostValidationRequest request = new BlogPostValidationRequest(3, 100, 10, 1000, blogPost);
        ValidationResponse validationResponse1 = restTemplate.postForEntity("http://localhost:" + port + "/api" +
                "/validate/blogpost", request, ValidationResponse.class).getBody();
        assertNotNull(validationResponse1);

        ValidationResponse validationResponse2 = restTemplate.postForEntity("http://localhost:" + port + "/api" +
                "/validate/blogpost", request, ValidationResponse.class).getBody();
        assertNotNull(validationResponse2);

        ValidationResponse validationResponse3 = restTemplate.postForEntity("http://localhost:" + port + "/api" +
                "/validate/blogpost", request, ValidationResponse.class).getBody();
        assertNotNull(validationResponse3);


        ResponseEntity<ValidationResultDto[]> response =
                restTemplate.getForEntity("http://localhost:" + port + "/api/users/" +
                        user.getId() + "/validation-results", ValidationResultDto[].class);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());

        Set<ValidationResultDto> actual= new HashSet<>(List.of(response.getBody()));
        Set<ValidationResultDto> expected = new HashSet<>(List.of(
            validationResponse1.getValidationResult(),
            validationResponse2.getValidationResult(),
            validationResponse3.getValidationResult()
        ));
        assertEquals(expected, actual);
    }


    @Test
    void givenNonExistingUser_getValidationResultsByUser_shouldReturnNotFoundErrorMessage() {
        UUID nonExistentUserId = UUID.randomUUID();

        ResponseEntity<CustomErrorResponse> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/users/" + nonExistentUserId + "/validation-results",
                CustomErrorResponse.class
        );

        assertEquals(404, response.getStatusCode().value());
        assertNotNull(response.getBody());
        CustomErrorResponse errorResponse = response.getBody();
        assertEquals("The entity User with id " + nonExistentUserId + " does not exist", errorResponse.getError());
    }

    @Test
    void givenOneBlogPostAndPersistedValidationPipeline_validateBlogPostsForUser_shouldReturnValidationResult() {
        User user = new User(UUID.randomUUID(), "Pipeline User", "pipelineuser-" + UUID.randomUUID() + "@example.com", Instant.now(), Instant.now(), uniqueUsername(), "TestUser-" + UUID.randomUUID(), true);
        userRepository.createUser(user);

        BlogPost blogPost = new BlogPost(UUID.randomUUID(), "Test Blog Post Title", "This is test content for the blog post", Instant.now(), Instant.now(), user.getId());
        blogPostRepository.createBlogPost(blogPost);

        var createPipelineDto = new ValidationPipelineCreateDto();
        createPipelineDto.setUserId(user.getId());
        createPipelineDto.setContentType("blogpost");
        createPipelineDto.setDescription("Test pipeline for blog post validation");
        createPipelineDto.setSteps(List.of(
                new ValidationStepDto(null, ValidationStepType.LENGTH_VALIDATION, "title",
                        Map.of("minLength", "5", "maxLength", "100"), true),
                new ValidationStepDto(null, ValidationStepType.LENGTH_VALIDATION, "content",
                        Map.of("minLength", "10", "maxLength", "1000"), true)
        ));

        restTemplate.postForEntity("http://localhost:" + port + "/api/validation-pipelines",
                createPipelineDto, ValidationPipelineModel.class);

        var response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/users/" + user.getId() + "/validate-blogposts",
                null, ValidationResponse[].class);

        assertEquals(200, response.getStatusCode().value());
        var results = response.getBody();
        assertNotNull(results);
        assertEquals(1, results.length);

        ValidationResponse validationResponse = results[0];
        assertEquals("BlogPost", validationResponse.getContentType());
        ValidationResultDto result = validationResponse.getValidationResult();
        assertEquals("BlogPost", result.getContentType());
        assertEquals(user.getId(), result.getUserId());
        assertTrue(result.isValid());
    }

    @Test
    void givenInvalidBlogPostAndPersistedValidationPipeline_validateBlogPostsForUser_shouldReturnValidationResultWithErrors() {
        User user = new User(UUID.randomUUID(), "Pipeline User", "pipelineuser-" + UUID.randomUUID() + "@example.com", Instant.now(), Instant.now(), uniqueUsername(), "TestUser-" + UUID.randomUUID(), true);
        userRepository.createUser(user);

        BlogPost blogPost = new BlogPost(UUID.randomUUID(), "Shrt", "Too short", Instant.now(), Instant.now(), user.getId());
        blogPostRepository.createBlogPost(blogPost);

        var createPipelineDto = new ValidationPipelineCreateDto();
        createPipelineDto.setUserId(user.getId());
        createPipelineDto.setContentType("blogpost");
        createPipelineDto.setDescription("Test pipeline for blog post validation");
        createPipelineDto.setSteps(List.of(
                new ValidationStepDto(null, ValidationStepType.LENGTH_VALIDATION, "title",
                        Map.of("minLength", "5", "maxLength", "100"), true),
                new ValidationStepDto(null, ValidationStepType.LENGTH_VALIDATION, "content",
                        Map.of("minLength", "10", "maxLength", "1000"), true)
        ));

        restTemplate.postForEntity("http://localhost:" + port + "/api/validation-pipelines",
                createPipelineDto, ValidationPipelineModel.class);

        var response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/users/" + user.getId() + "/validate-blogposts",
                null, ValidationResponse[].class);

        assertEquals(200, response.getStatusCode().value());
        var results = response.getBody();
        assertNotNull(results);
        assertEquals(1, results.length);

        ValidationResponse validationResponse = results[0];
        assertEquals("BlogPost", validationResponse.getContentType());
        ValidationResultDto result = validationResponse.getValidationResult();
        assertEquals("BlogPost", result.getContentType());
        assertEquals(user.getId(), result.getUserId());
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
    void givenMultipleBlogPostsWithViolationsAndPersistedValidationPipeline_validateBlogPostsForUser_shouldReturnAllValidationResults() {
        User user = new User(UUID.randomUUID(), "Pipeline User", "pipelineuser-" + UUID.randomUUID() + "@example.com", Instant.now(), Instant.now(), uniqueUsername(), "TestUser-" + UUID.randomUUID(), true);
        userRepository.createUser(user);

        BlogPost blogPost1 = new BlogPost(UUID.randomUUID(), "Hi", "This is valid content for the first blog post", Instant.now(), Instant.now(), user.getId());
        blogPostRepository.createBlogPost(blogPost1);

        BlogPost blogPost2 = new BlogPost(UUID.randomUUID(), "Valid Title Here", "Short", Instant.now(), Instant.now(), user.getId());
        blogPostRepository.createBlogPost(blogPost2);

        BlogPost blogPost3 = new BlogPost(UUID.randomUUID(), "Bad", "Bad", Instant.now(), Instant.now(), user.getId());
        blogPostRepository.createBlogPost(blogPost3);

        var createPipelineDto = new ValidationPipelineCreateDto();
        createPipelineDto.setUserId(user.getId());
        createPipelineDto.setContentType("blogpost");
        createPipelineDto.setDescription("Test pipeline for multiple blog post validation");
        createPipelineDto.setSteps(List.of(
                new ValidationStepDto(null, ValidationStepType.LENGTH_VALIDATION, "title",
                        Map.of("minLength", "5", "maxLength", "100"), true),
                new ValidationStepDto(null, ValidationStepType.LENGTH_VALIDATION, "content",
                        Map.of("minLength", "10", "maxLength", "1000"), true)
        ));

        restTemplate.postForEntity("http://localhost:" + port + "/api/validation-pipelines",
                createPipelineDto, ValidationPipelineModel.class);

        var response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/users/" + user.getId() + "/validate-blogposts",
                null, ValidationResponse[].class);

        assertEquals(200, response.getStatusCode().value());
        var results = response.getBody();
        assertNotNull(results);
        assertEquals(3, results.length);

        for (ValidationResponse validationResponse : results) {
            assertEquals("BlogPost", validationResponse.getContentType());
            ValidationResultDto result = validationResponse.getValidationResult();
            assertEquals("BlogPost", result.getContentType());
            assertEquals(user.getId(), result.getUserId());
            assertFalse(result.isValid());
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

    @Test
    void givenOneBlogPostAndPipeline_whenValidateBlogPostsForUser_thenResultPersistedInDatabase() {
        User user = new User(UUID.randomUUID(), "Persist User", "persistuser-" + UUID.randomUUID() + "@example.com", Instant.now(), Instant.now(), uniqueUsername(), "TestUser-" + UUID.randomUUID(), true);
        userRepository.createUser(user);

        BlogPost blogPost = new BlogPost(UUID.randomUUID(), "Valid Title", "This is sufficiently long content", Instant.now(), Instant.now(), user.getId());
        blogPostRepository.createBlogPost(blogPost);

        var createPipelineDto = new ValidationPipelineCreateDto();
        createPipelineDto.setUserId(user.getId());
        createPipelineDto.setContentType("blogpost");
        createPipelineDto.setDescription("Persistence test pipeline");
        createPipelineDto.setSteps(List.of(
                new ValidationStepDto(null, ValidationStepType.LENGTH_VALIDATION, "title", Map.of("minLength", "5", "maxLength", "100"), true),
                new ValidationStepDto(null, ValidationStepType.LENGTH_VALIDATION, "content", Map.of("minLength", "10", "maxLength", "1000"), true)
        ));
        restTemplate.postForEntity("http://localhost:" + port + "/api/validation-pipelines", createPipelineDto, ValidationPipelineModel.class);;

        var validateResponse = restTemplate.postForEntity("http://localhost:" + port + "/api/users/" + user.getId() + "/validate-blogposts", null, ValidationResponse[].class);
        assertEquals(200, validateResponse.getStatusCode().value());
        assertNotNull(validateResponse.getBody());
        assertEquals(1, validateResponse.getBody().length);

        List<ValidationResult> persisted = validationResultRepository.findByUserId(user.getId());
        assertEquals(1, persisted.size());
        ValidationResult result = persisted.getFirst();
        assertEquals(user.getId(), result.getUserId());
        assertEquals(blogPost.getId(), result.getContentId());
        assertTrue(result.isValid());
        assertEquals(0, result.getErrors().size());
    }

    @Test
    void givenMultipleBlogPostsAndPipeline_whenValidateBlogPostsForUser_thenAllResultsPersistedInDatabase() {
        User user = new User(UUID.randomUUID(), "Persist Multi User", "persistmulti-" + UUID.randomUUID() + "@example.com", Instant.now(), Instant.now(), uniqueUsername(), "TestUser-" + UUID.randomUUID(), true);
        userRepository.createUser(user);

        BlogPost validPost = new BlogPost(UUID.randomUUID(), "Valid Title", "This content is definitely long enough", Instant.now(), Instant.now(), user.getId());
        BlogPost shortTitlePost = new BlogPost(UUID.randomUUID(), "Bad", "Content that is long enough for validation", Instant.now(), Instant.now(), user.getId());
        BlogPost shortContentPost = new BlogPost(UUID.randomUUID(), "Another Valid Title", "Short", Instant.now(), Instant.now(), user.getId());
        BlogPost bothShortPost = new BlogPost(UUID.randomUUID(), "No", "Bad", Instant.now(), Instant.now(), user.getId());
        blogPostRepository.createBlogPost(validPost);
        blogPostRepository.createBlogPost(shortTitlePost);
        blogPostRepository.createBlogPost(shortContentPost);
        blogPostRepository.createBlogPost(bothShortPost);

        var createPipelineDto = new ValidationPipelineCreateDto();
        createPipelineDto.setUserId(user.getId());
        createPipelineDto.setContentType("blogpost");
        createPipelineDto.setDescription("Persistence multi pipeline");
        createPipelineDto.setSteps(List.of(
                new ValidationStepDto(null, ValidationStepType.LENGTH_VALIDATION, "title", Map.of("minLength", "5", "maxLength", "100"), true),
                new ValidationStepDto(null, ValidationStepType.LENGTH_VALIDATION, "content", Map.of("minLength", "10", "maxLength", "1000"), true)
        ));
        var pipelineCreateResponse = restTemplate.postForEntity("http://localhost:" + port + "/api/validation-pipelines", createPipelineDto, ValidationPipelineModel.class);

        var validateResponse = restTemplate.postForEntity("http://localhost:" + port + "/api/users/" + user.getId() + "/validate-blogposts", null, ValidationResponse[].class);
        assertEquals(200, validateResponse.getStatusCode().value());
        assertNotNull(validateResponse.getBody());
        assertEquals(4, validateResponse.getBody().length);

        List<ValidationResult> persisted = validationResultRepository.findByUserId(user.getId());
        assertEquals(4, persisted.size());

        Map<UUID, ValidationResult> byContentId = new HashMap<>();
        for(ValidationResult r : persisted) {
            byContentId.put(r.getContentId(), r);
        }
        assertTrue(byContentId.get(validPost.getId()).isValid());
        assertTrue(byContentId.get(validPost.getId()).getErrors().isEmpty());

        ValidationResult titleResult = byContentId.get(shortTitlePost.getId());
        assertFalse(titleResult.isValid());
        assertTrue(titleResult.getErrors().stream().anyMatch(e -> e.code().equals(LengthValidator.ERROR_CODE) && e.message().equals(LengthValidator.errorMessageTooShort("title"))));

        ValidationResult contentResult = byContentId.get(shortContentPost.getId());
        assertFalse(contentResult.isValid());
        assertTrue(contentResult.getErrors().stream().anyMatch(e -> e.code().equals(LengthValidator.ERROR_CODE) && e.message().equals(LengthValidator.errorMessageTooShort("content"))));

        ValidationResult bothResult = byContentId.get(bothShortPost.getId());
        assertFalse(bothResult.isValid());
        assertEquals(2, bothResult.getErrors().size());
        assertTrue(bothResult.getErrors().stream().anyMatch(e -> e.code().equals(LengthValidator.ERROR_CODE) && e.message().equals(LengthValidator.errorMessageTooShort("title"))));
        assertTrue(bothResult.getErrors().stream().anyMatch(e -> e.code().equals(LengthValidator.ERROR_CODE) && e.message().equals(LengthValidator.errorMessageTooShort("content"))));
    }

    @Test
    void givenMultipleValidationResults_whenGetValidationReport_thenReturnCorrectReport() {
        User user = new User(UUID.randomUUID(), "Report User", "reportuser-" + UUID.randomUUID() + "@example.com", Instant.now(), Instant.now(), uniqueUsername(), "TestUser-" + UUID.randomUUID(), true);
        userRepository.createUser(user);

        BlogPost phoneNumberIncludedInPost = new BlogPost(UUID.randomUUID(), "Valid Title", "This content is sufficiently long, call +1 234 567 8901", Instant.now(), Instant.now(), user.getId());
        BlogPost shortTitlePost = new BlogPost(UUID.randomUUID(), "Bad", "This content is sufficiently long", Instant.now(), Instant.now(), user.getId());
        BlogPost shortContentPost = new BlogPost(UUID.randomUUID(), "Another Valid Title", "Short", Instant.now(), Instant.now(), user.getId());
        blogPostRepository.createBlogPost(phoneNumberIncludedInPost);
        blogPostRepository.createBlogPost(shortTitlePost);
        blogPostRepository.createBlogPost(shortContentPost);

        var createPipelineDto = new ValidationPipelineCreateDto();
        createPipelineDto.setUserId(user.getId());
        createPipelineDto.setContentType("blogpost");
        createPipelineDto.setDescription("Report pipeline");
        createPipelineDto.setSteps(List.of(
                new ValidationStepDto(null, ValidationStepType.LENGTH_VALIDATION, "title",
                        Map.of("minLength", "5", "maxLength", "100"), true),
                new ValidationStepDto(null, ValidationStepType.LENGTH_VALIDATION, "content",
                        Map.of("minLength", "10", "maxLength", "1000"), true),
                new ValidationStepDto(null, ValidationStepType.PHONE_NUMBER_FORBIDDEN_VALIDATION, "content",
                        Map.of(), true)
        ));

        var pipelineResponse = restTemplate.postForEntity("http://localhost:" + port + "/api/validation-pipelines",
                createPipelineDto, ValidationPipelineModel.class);
        assertEquals(201, pipelineResponse.getStatusCode().value());
        assertNotNull(pipelineResponse.getBody());

        var validateResponse = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/users/" + user.getId() + "/validate-blogposts",
                null, ValidationResponse[].class);
        assertEquals(200, validateResponse.getStatusCode().value());
        assertNotNull(validateResponse.getBody());
        assertEquals(3, validateResponse.getBody().length);

        ResponseEntity<ValidationReportDto> reportResponse = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/users/" + user.getId() + "/validation-report",
                ValidationReportDto.class);
        assertEquals(200, reportResponse.getStatusCode().value());
        assertNotNull(reportResponse.getBody());
        ValidationReportDto report = reportResponse.getBody();

        assertEquals(3, report.getTotalErrorCount());
        assertEquals("2", report.getErrorCodeToErrorCount().get(LengthValidator.ERROR_CODE));
        assertEquals("1", report.getErrorCodeToErrorCount().get(PhoneNumberForbiddenValidator.ERROR_CODE));
        assertEquals(2, report.getErrorCodeToErrorCount().size());
    }

    @Test
    void givenValidationResultsForMultipleUsers_whenGetValidationReportForUser_thenReturnOnlyRequestedUsersCounts() {
        User user1 = new User(UUID.randomUUID(), "Report User A", "reportA-" + UUID.randomUUID() + "@example.com", Instant.now(), Instant.now(), uniqueUsername(), "TestUser-" + UUID.randomUUID(), true);
        User user2 = new User(UUID.randomUUID(), "Report User B", "reportB-" + UUID.randomUUID() + "@example.com", Instant.now(), Instant.now(), uniqueUsername(), "TestUser-" + UUID.randomUUID(), true);
        userRepository.createUser(user1);
        userRepository.createUser(user2);

        BlogPost user1Valid = new BlogPost(UUID.randomUUID(), "Valid Title", "This content is sufficiently long", Instant.now(), Instant.now(), user1.getId());
        BlogPost user1InvalidTitle = new BlogPost(UUID.randomUUID(), "Bad", "This content is sufficiently long", Instant.now(), Instant.now(), user1.getId());
        BlogPost user2InvalidContent = new BlogPost(UUID.randomUUID(), "Another Valid Title", "Short", Instant.now(), Instant.now(), user2.getId());
        blogPostRepository.createBlogPost(user1Valid);
        blogPostRepository.createBlogPost(user1InvalidTitle);
        blogPostRepository.createBlogPost(user2InvalidContent);

        var pipelineDtoUser1 = new ValidationPipelineCreateDto();
        pipelineDtoUser1.setUserId(user1.getId());
        pipelineDtoUser1.setContentType("blogpost");
        pipelineDtoUser1.setDescription("Report pipeline user1");
        pipelineDtoUser1.setSteps(List.of(
                new ValidationStepDto(null, ValidationStepType.LENGTH_VALIDATION, "title", Map.of("minLength", "5", "maxLength", "100"), true),
                new ValidationStepDto(null, ValidationStepType.LENGTH_VALIDATION, "content", Map.of("minLength", "10", "maxLength", "1000"), true)
        ));
        restTemplate.postForEntity("http://localhost:" + port + "/api/validation-pipelines", pipelineDtoUser1, ValidationPipelineModel.class);

        var pipelineDtoUser2 = new ValidationPipelineCreateDto();
        pipelineDtoUser2.setUserId(user2.getId());
        pipelineDtoUser2.setContentType("blogpost");
        pipelineDtoUser2.setDescription("Report pipeline user2");
        pipelineDtoUser2.setSteps(List.of(
                new ValidationStepDto(null, ValidationStepType.LENGTH_VALIDATION, "title", Map.of("minLength", "5", "maxLength", "100"), true),
                new ValidationStepDto(null, ValidationStepType.LENGTH_VALIDATION, "content", Map.of("minLength", "10", "maxLength", "1000"), true)
        ));
        restTemplate.postForEntity("http://localhost:" + port + "/api/validation-pipelines", pipelineDtoUser2, ValidationPipelineModel.class);

        restTemplate.postForEntity("http://localhost:" + port + "/api/users/" + user1.getId() + "/validate-blogposts", null, ValidationResponse[].class);
        restTemplate.postForEntity("http://localhost:" + port + "/api/users/" + user2.getId() + "/validate-blogposts", null, ValidationResponse[].class);

        ResponseEntity<ValidationReportDto> reportResponse = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/users/" + user1.getId() + "/validation-report",
                ValidationReportDto.class);
        assertEquals(200, reportResponse.getStatusCode().value());
        assertNotNull(reportResponse.getBody());
        ValidationReportDto report = reportResponse.getBody();

        assertEquals(1, report.getTotalErrorCount());
        assertEquals("1", report.getErrorCodeToErrorCount().get(LengthValidator.ERROR_CODE));
        assertEquals(1, report.getErrorCodeToErrorCount().size());
    }

    // -------------------------------------------------------------------------
    // 1. SUCCESS — valid support request returns a valid result
    // -------------------------------------------------------------------------
    @Test
    void givenValidSupportRequest_whenValidate_thenReturnValidResult() {

        // Create user
        User user = new User(
                UUID.randomUUID(),
                "Support User",
                "support-" + UUID.randomUUID() + "@example.com",
                Instant.now(),
                Instant.now(),
                uniqueUsername(),
                "TestUser-" + UUID.randomUUID(),
                true
        );
        userRepository.createUser(user);

        // Create support request
        SupportRequest request = new SupportRequest(
                UUID.randomUUID(),
                user.getId(),
                "This is a valid support request message.",
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.now(),
                Instant.now()
        );
        supportRequestRepository.create(request);

        // Create validation pipeline
        var pipelineDto = new ValidationPipelineCreateDto();
        pipelineDto.setUserId(user.getId());
        pipelineDto.setContentType("supportrequest");
        pipelineDto.setDescription("Support Request Validation Pipeline");
        pipelineDto.setSteps(List.of(
                new ValidationStepDto(null, ValidationStepType.LENGTH_VALIDATION, "text",
                        Map.of("minLength", "5", "maxLength", "500"), true)
        ));

        restTemplate.postForEntity(
                "http://localhost:" + port + "/api/validation-pipelines",
                pipelineDto,
                ValidationPipelineModel.class
        );

        // Call the API
        ResponseEntity<ValidationResponse[]> response =
                restTemplate.postForEntity(
                        "http://localhost:" + port + "/api/users/" + user.getId() + "/validate-supportrequests",
                        null,
                        ValidationResponse[].class
                );

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().length);

        ValidationResponse vr = response.getBody()[0];
        assertEquals("SupportRequest", vr.getContentType());

        ValidationResultDto dto = vr.getValidationResult();
        assertTrue(dto.isValid());
        assertEquals("SupportRequest", dto.getContentType());
        assertEquals(user.getId(), dto.getUserId());
        assertEquals(request.getId(), dto.getContentId());
    }

    // -------------------------------------------------------------------------
    // 2. FAILURE — invalid support request returns validation errors
    // -------------------------------------------------------------------------
    @Test
    void givenInvalidSupportRequest_whenValidate_thenReturnErrors() {

        User user = new User(
                UUID.randomUUID(),
                "Invalid Support User",
                "invalid-sr-" + UUID.randomUUID() + "@example.com",
                Instant.now(),
                Instant.now(),
                uniqueUsername(),
                "TestUser-" + UUID.randomUUID(),
                true
        );
        userRepository.createUser(user);

        // Too short text to trigger validation errors
        SupportRequest request = new SupportRequest(
                UUID.randomUUID(),
                user.getId(),
                "Hi",
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.now(),
                Instant.now()
        );
        supportRequestRepository.create(request);

        // Create pipeline for validation
        var pipelineDto = new ValidationPipelineCreateDto();
        pipelineDto.setUserId(user.getId());
        pipelineDto.setContentType("supportrequest");
        pipelineDto.setDescription("SR Pipeline");
        pipelineDto.setSteps(List.of(
                new ValidationStepDto(null, ValidationStepType.LENGTH_VALIDATION, "text",
                        Map.of("minLength", "5", "maxLength", "500"), true)
        ));

        restTemplate.postForEntity(
                "http://localhost:" + port + "/api/validation-pipelines",
                pipelineDto,
                ValidationPipelineModel.class
        );

        ResponseEntity<ValidationResponse[]> response =
                restTemplate.postForEntity(
                        "http://localhost:" + port + "/api/users/" + user.getId() + "/validate-supportrequests",
                        null,
                        ValidationResponse[].class
                );

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().length);

        ValidationResultDto dto = response.getBody()[0].getValidationResult();
        assertFalse(dto.isValid());
        assertEquals(1, dto.getErrors().size());

        assertTrue(dto.getErrors().stream().anyMatch(
                e -> e.code().equals(LengthValidator.ERROR_CODE)
        ));
    }

    // -------------------------------------------------------------------------
    // 3. VALIDATION RESULTS ARE PERSISTED
    // -------------------------------------------------------------------------
    @Test
    void givenSupportRequest_whenValidate_thenValidationIsPersisted() {

        User user = new User(
                UUID.randomUUID(),
                "Persist SR User",
                "persistsr-" + UUID.randomUUID() + "@example.com",
                Instant.now(),
                Instant.now(),
                uniqueUsername(),
                "TestUser-" + UUID.randomUUID(),
                true
        );
        userRepository.createUser(user);

        SupportRequest sr = new SupportRequest(
                UUID.randomUUID(),
                user.getId(),
                "This is a valid request",
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.now(),
                Instant.now()
        );
        supportRequestRepository.create(sr);

        var pipelineDto = new ValidationPipelineCreateDto();
        pipelineDto.setUserId(user.getId());
        pipelineDto.setContentType("supportrequest");
        pipelineDto.setDescription("Persistence Pipeline");
        pipelineDto.setSteps(List.of(
                new ValidationStepDto(null, ValidationStepType.LENGTH_VALIDATION, "text",
                        Map.of("minLength", "5", "maxLength", "500"), true)
        ));

        restTemplate.postForEntity(
                "http://localhost:" + port + "/api/validation-pipelines",
                pipelineDto,
                ValidationPipelineModel.class
        );

        restTemplate.postForEntity(
                "http://localhost:" + port + "/api/users/" + user.getId() + "/validate-supportrequests",
                null,
                ValidationResponse[].class
        );

        List<ValidationResult> persisted = validationResultRepository.findByUserId(user.getId());
        assertEquals(1, persisted.size());
        assertEquals(user.getId(), persisted.get(0).getUserId());
        assertEquals(sr.getId(), persisted.get(0).getContentId());
    }

    @Test
    void givenOneSupportResponseAndPersistedValidationPipeline_validateSupportResponsesForUser_shouldReturnValidationResult() {
        User user = new User(UUID.randomUUID(), "Support User", "support-" + UUID.randomUUID() + "@example.com", Instant.now(), Instant.now(), uniqueUsername(), "TestUser-" + UUID.randomUUID(), true);
        userRepository.createUser(user);

        SupportRequest supportRequest = new SupportRequest(UUID.randomUUID(), user.getId(), "Help needed", null, user.getId(), Instant.now(), Instant.now());
        supportRequestRepository.create(supportRequest);

        SupportResponse supportResponse = new SupportResponse(UUID.randomUUID(), user.getId(), "We have resolved your issue. Thank you.", supportRequest.getId(), Instant.now(), Instant.now());
        supportResponseRepository.create(supportResponse);

        ValidationPipelineCreateDto pipelineDto = new ValidationPipelineCreateDto();
        pipelineDto.setUserId(user.getId());
        pipelineDto.setContentType("supportresponse");
        pipelineDto.setDescription("Test pipeline for support response validation");
        pipelineDto.setSteps(List.of(
                new ValidationStepDto(null, ValidationStepType.LENGTH_VALIDATION, "text",
                        Map.of("minLength", "10", "maxLength", "500"), true)
        ));
        restTemplate.postForEntity("http://localhost:" + port + "/api/validation-pipelines", pipelineDto, ValidationPipelineModel.class);

        ResponseEntity<ValidationResponse[]> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/users/" + user.getId() + "/validate-supportresponses",
                null, ValidationResponse[].class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        ValidationResponse[] results = response.getBody();
        assertNotNull(results);
        assertEquals(1, results.length);

        ValidationResponse validationResponse = results[0];
        assertEquals("SupportResponse", validationResponse.getContentType());

        ValidationResultDto result = validationResponse.getValidationResult();
        assertEquals("SupportResponse", result.getContentType());
        assertEquals(user.getId(), result.getUserId());
        assertTrue(result.isValid());
    }

    @Test
    void givenInvalidSupportResponseAndPersistedValidationPipeline_validateSupportResponsesForUser_shouldReturnValidationResultWithErrors() {
        User user = new User(UUID.randomUUID(), "Support User", "support-" + UUID.randomUUID() + "@example.com", Instant.now(), Instant.now(), uniqueUsername(), "TestUser-" + UUID.randomUUID(), true);
        userRepository.createUser(user);

        SupportRequest supportRequest = new SupportRequest(UUID.randomUUID(), user.getId(), "Help", null, user.getId(), Instant.now(), Instant.now());
        supportRequestRepository.create(supportRequest);

        SupportResponse supportResponse = new SupportResponse(UUID.randomUUID(), user.getId(), "Hi", supportRequest.getId(), Instant.now(), Instant.now());
        supportResponseRepository.create(supportResponse);

        ValidationPipelineCreateDto pipelineDto = new ValidationPipelineCreateDto();
        pipelineDto.setUserId(user.getId());
        pipelineDto.setContentType("supportresponse");
        pipelineDto.setDescription("Test pipeline for support response validation");
        pipelineDto.setSteps(List.of(
                new ValidationStepDto(null, ValidationStepType.LENGTH_VALIDATION, "text",
                        Map.of("minLength", "10", "maxLength", "500"), true)
        ));
        restTemplate.postForEntity("http://localhost:" + port + "/api/validation-pipelines", pipelineDto, ValidationPipelineModel.class);

        ResponseEntity<ValidationResponse[]> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/users/" + user.getId() + "/validate-supportresponses",
                null, ValidationResponse[].class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        ValidationResponse[] results = response.getBody();
        assertNotNull(results);
        assertEquals(1, results.length);

        ValidationResponse validationResponse = results[0];
        assertEquals("SupportResponse", validationResponse.getContentType());

        ValidationResultDto result = validationResponse.getValidationResult();
        assertEquals("SupportResponse", result.getContentType());
        assertEquals(user.getId(), result.getUserId());
        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());

        assertTrue(result.getErrors().stream().anyMatch(
                e -> e.code().equals(LengthValidator.ERROR_CODE) &&
                        e.message().equals(LengthValidator.errorMessageTooShort("text"))
        ));
    }

    @Test
    void givenOneSupportResponseAndPipeline_whenValidateSupportResponsesForUser_thenResultPersistedInDatabase() {
        User user = new User(UUID.randomUUID(), "Support User", "support-" + UUID.randomUUID() + "@example.com", Instant.now(), Instant.now(), uniqueUsername(), "TestUser-" + UUID.randomUUID(), true);
        userRepository.createUser(user);

        SupportRequest supportRequest = new SupportRequest(UUID.randomUUID(), user.getId(), "Help", null, user.getId(), Instant.now(), Instant.now());
        supportRequestRepository.create(supportRequest);

        SupportResponse supportResponse = new SupportResponse(UUID.randomUUID(), user.getId(), "Valid response text", supportRequest.getId(), Instant.now(), Instant.now());
        supportResponseRepository.create(supportResponse);

        ValidationPipelineCreateDto pipelineDto = new ValidationPipelineCreateDto();
        pipelineDto.setUserId(user.getId());
        pipelineDto.setContentType("supportresponse");
        pipelineDto.setSteps(List.of(
                new ValidationStepDto(null, ValidationStepType.LENGTH_VALIDATION, "text",
                        Map.of("minLength", "5", "maxLength", "500"), true)
        ));
        restTemplate.postForEntity("http://localhost:" + port + "/api/validation-pipelines", pipelineDto, ValidationPipelineModel.class);

        restTemplate.postForEntity("http://localhost:" + port + "/api/users/" + user.getId() + "/validate-supportresponses", null, ValidationResponse[].class);

        List<ValidationResult> persisted = validationResultRepository.findByUserId(user.getId());
        assertEquals(1, persisted.size());
        ValidationResult result = persisted.get(0);
        assertEquals(user.getId(), result.getUserId());
        assertEquals(supportResponse.getId(), result.getContentId());
        assertTrue(result.isValid());
    }

    @Test
    void givenMultipleSupportResponsesAndPipeline_whenValidateSupportResponsesForUser_thenAllResultsPersistedInDatabase() {
        User user = new User(UUID.randomUUID(), "Support User", "support-" + UUID.randomUUID() + "@example.com", Instant.now(), Instant.now(), uniqueUsername(), "TestUser-" + UUID.randomUUID(), true);
        userRepository.createUser(user);

        SupportRequest request1 = new SupportRequest(UUID.randomUUID(), user.getId(), "Issue 1", null, user.getId(), Instant.now(), Instant.now());
        SupportRequest request2 = new SupportRequest(UUID.randomUUID(), user.getId(), "Issue 2", null, user.getId(), Instant.now(), Instant.now());
        supportRequestRepository.create(request1);
        supportRequestRepository.create(request2);

        SupportResponse response1 = new SupportResponse(UUID.randomUUID(), user.getId(), "Valid response", request1.getId(), Instant.now(), Instant.now());
        SupportResponse response2 = new SupportResponse(UUID.randomUUID(), user.getId(), "Short", request2.getId(), Instant.now(), Instant.now());
        supportResponseRepository.create(response1);
        supportResponseRepository.create(response2);

        ValidationPipelineCreateDto pipelineDto = new ValidationPipelineCreateDto();
        pipelineDto.setUserId(user.getId());
        pipelineDto.setContentType("supportresponse");
        pipelineDto.setSteps(List.of(
                new ValidationStepDto(null, ValidationStepType.LENGTH_VALIDATION, "text",
                        Map.of("minLength", "10", "maxLength", "500"), true)
        ));
        restTemplate.postForEntity("http://localhost:" + port + "/api/validation-pipelines", pipelineDto, ValidationPipelineModel.class);

        restTemplate.postForEntity("http://localhost:" + port + "/api/users/" + user.getId() + "/validate-supportresponses", null, ValidationResponse[].class);

        List<ValidationResult> persisted = validationResultRepository.findByUserId(user.getId());
        assertEquals(2, persisted.size());

        Map<UUID, ValidationResult> byContentId = new HashMap<>();
        for (ValidationResult r : persisted) {
            byContentId.put(r.getContentId(), r);
        }

        assertTrue(byContentId.get(response1.getId()).isValid());
        assertFalse(byContentId.get(response2.getId()).isValid());
    }

    @Test
    void givenMultipleSupportResponsesWithViolationsAndPersistedValidationPipeline_validateSupportResponsesForUser_shouldReturnAllValidationResults() throws JsonMappingException, JsonProcessingException {
        // ---------- Arrange ----------
        User user = new User(UUID.randomUUID(),
                "Support User",
                "support-" + UUID.randomUUID() + "@example.com",
                Instant.now(), Instant.now(), uniqueUsername(), "TestUser-" + UUID.randomUUID(), true);
        userRepository.createUser(user);

        SupportRequest r1 = new SupportRequest(UUID.randomUUID(), user.getId(), "Issue 1", null, user.getId(), Instant.now(), Instant.now());
        SupportRequest r2 = new SupportRequest(UUID.randomUUID(), user.getId(), "Issue 2", null, user.getId(), Instant.now(), Instant.now());
        SupportRequest r3 = new SupportRequest(UUID.randomUUID(), user.getId(), "Issue 3", null, user.getId(), Instant.now(), Instant.now());
        supportRequestRepository.create(r1);
        supportRequestRepository.create(r2);
        supportRequestRepository.create(r3);


        SupportResponse valid = new SupportResponse(UUID.randomUUID(), user.getId(),
                "This is a valid response text.", r1.getId(), Instant.now(), Instant.now());

        SupportResponse short1 = new SupportResponse(UUID.randomUUID(), user.getId(),
                "Hi", r2.getId(), Instant.now(), Instant.now());

        SupportResponse short2 = new SupportResponse(UUID.randomUUID(), user.getId(),
                "A", r3.getId(), Instant.now(), Instant.now());

        supportResponseRepository.create(valid);
        supportResponseRepository.create(short1);
        supportResponseRepository.create(short2);

        ValidationPipelineCreateDto pipelineDto = new ValidationPipelineCreateDto();
        pipelineDto.setUserId(user.getId());
        pipelineDto.setContentType("supportresponse");
        pipelineDto.setDescription("Min length validation");
        Map<String, String> lengthParams = new HashMap<>();
        lengthParams.put("minLength", "10");
        lengthParams.put("maxLength", "1000");
        pipelineDto.setSteps(List.of(
                new ValidationStepDto(UUID.randomUUID(),   // ← dummy UUID (required)
                        ValidationStepType.LENGTH_VALIDATION,
                        "text",
                        lengthParams,   // ← only minLength is checked
                        true)
        ));

        ResponseEntity<ValidationPipelineModel> pipelineResponse = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/validation-pipelines",
                pipelineDto,
                ValidationPipelineModel.class);

        assertEquals(HttpStatus.CREATED, pipelineResponse.getStatusCode(),
                "Pipeline creation failed during setup. Check server logs for validation errors on pipelineDto.");

        ResponseEntity<ValidationResponse[]> response = restTemplate.exchange(
                "http://localhost:" + port + "/api/users/" + user.getId() + "/validate-supportresponses",
                HttpMethod.POST,
                null,
                ValidationResponse[].class
        );

        // ---------- Assert ----------
        assertEquals(HttpStatus.OK, response.getStatusCode(),
                "Validation API call failed. Check server logs for the 400 BAD_REQUEST root cause.");

        ValidationResponse[] body = response.getBody();
        assertNotNull(body);

        assertEquals(3, body.length);

        int validCount = 0;
        int shortCount = 0;

        for (ValidationResponse vr : body) {
            ValidationResultDto dto = vr.getValidationResult();
            assertEquals("SupportResponse", dto.getContentType());
            assertEquals(user.getId(), dto.getUserId());

            if (dto.isValid()) {
                validCount++;
            } else {
                assertEquals(1, dto.getErrors().size());
                ValidationError err = dto.getErrors().get(0);
                assertEquals(LengthValidator.ERROR_CODE, err.code());
                assertEquals(LengthValidator.errorMessageTooShort("text"), err.message());
                shortCount++;
            }
        }

        assertEquals(1, validCount);   // only the first response
        assertEquals(2, shortCount);   // two short responses
    }

}
