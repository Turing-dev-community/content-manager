package com.dehold.contentmanager.user.web;

import com.dehold.contentmanager.content.blogpost.model.BlogPost;
import com.dehold.contentmanager.content.blogpost.service.BlogPostService;
import com.dehold.contentmanager.content.customersupport.model.SupportResponse;
import com.dehold.contentmanager.content.webhook.model.Webhook;
import com.dehold.contentmanager.content.webhook.web.dto.CreateWebhookRequest;
import com.dehold.contentmanager.content.webhook.web.dto.UpdateWebhookRequest;
import com.dehold.contentmanager.user.service.UserService;
import com.dehold.contentmanager.user.web.dto.CreateUserRequest;
import com.dehold.contentmanager.user.web.dto.UserResponse;
import com.dehold.contentmanager.user.web.dto.UpdateUserRequest;
import com.dehold.contentmanager.validation.model.ValidationResult;
import com.dehold.contentmanager.validation.service.ValidationService;
import com.dehold.contentmanager.content.webhook.service.WebhookService;
import com.dehold.contentmanager.validation.web.dto.ValidationReportDto;
import com.dehold.contentmanager.validation.web.dto.ValidationResponse;
import com.dehold.contentmanager.validation.web.dto.ValidationResultDto;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@Validated
public class UserController {

    private final UserService userService;
    private final BlogPostService blogPostService;
    private final ValidationService validationService;
    private final WebhookService webhookService;

    public UserController(UserService userService, BlogPostService blogPostService, ValidationService validationService,WebhookService webhookService) {
        this.userService = userService;
        this.blogPostService = blogPostService;
        this.validationService = validationService;
        this.webhookService = webhookService;
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@RequestBody CreateUserRequest request) {
        var user = userService.createUser(request);
        var response = new UserResponse(
                user.getId(),
                user.getAlias(),
                user.getEmail(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable UUID id) {
        var user = userService.getUser(id);
        var response = new UserResponse(
                user.getId(),
                user.getAlias(),
                user.getEmail(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/blogposts")
    public ResponseEntity<List<BlogPost>> getBlogpostsByUserId(@PathVariable UUID id) {
        userService.getUser(id); // Check for the user to be existent
        List<BlogPost> blogPosts = this.blogPostService.getBlogPostsByUserId(id);
        return ResponseEntity.ok(blogPosts);
    }

    @GetMapping("/{id}/validation-results")
    public ResponseEntity<List<ValidationResultDto>> getValidationResultsByUserId(@PathVariable UUID id) {
        userService.getUser(id); // Check for the user to be existent. Throws if not existent.
        List<ValidationResult> validationResults = validationService.findByUserId(id);
        List<ValidationResultDto> validationResultDtos = validationResults.stream().map(ValidationResultDto::from).toList();
        return ResponseEntity.ok(validationResultDtos);
    }

    @GetMapping("/{id}/validation-report")
    public ResponseEntity<ValidationReportDto> getValidationReportByUserId(@PathVariable UUID id) {
        userService.getUser(id); // Check for the user to be existent
        ValidationReportDto report = validationService.generateValidationReport(id);
        return ResponseEntity.ok(report);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable UUID id, @RequestBody UpdateUserRequest request) {
        var user = userService.updateUser(id, request);
        var response = new UserResponse(
                user.getId(),
                user.getAlias(),
                user.getEmail(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/validate-blogposts")
    public ResponseEntity<List<ValidationResponse>> validateBlogPostsForUser(@PathVariable UUID id) {
        userService.getUser(id); // Check for the user to be existent
        List<ValidationResult> results = validationService.runBlogPostValidation(id);
        List<ValidationResponse> response = results.stream()
                .map(ValidationResultDto::from)
                .map(dto -> new ValidationResponse(BlogPost.class.getSimpleName(), dto))
                .toList();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/{id}/validate-supportresponses")
    public ResponseEntity<List<ValidationResponse>> validateSupportResponsesForUser(@PathVariable UUID id) {
        userService.getUser(id); // Check for the user to be existent
        List<ValidationResult> results = validationService.runSupportResponseValidation(id);
        List<ValidationResponse> response = results.stream()
                .map(ValidationResultDto::from)
                .map(dto -> new ValidationResponse(SupportResponse.class.getSimpleName(), dto))
                .toList();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/{id}/validate-supportrequests")
    public ResponseEntity<List<ValidationResponse>> validateSupportRequests(@PathVariable UUID id) {
        userService.getUser(id);
        List<ValidationResult> results = validationService.runSupportRequestValidation(id);
        List<ValidationResponse> responses = results.stream()
                .map(result -> new ValidationResponse("SupportRequest", ValidationResultDto.from(result)))
                .toList();
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/{id}/validate-genericcontent")
    public ResponseEntity<List<ValidationResponse>> validateGenericContent(
            @PathVariable UUID id,
            @RequestParam(required = true) String contentType) {
        userService.getUser(id);
        List<ValidationResult> results = validationService.runSupportRequestValidation(id);
        List<ValidationResponse> responses = results.stream()
                .map(result -> new ValidationResponse("SupportRequest", ValidationResultDto.from(result)))
                .toList();
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/{userId}/webhooks")
    public ResponseEntity<Webhook> createWebhook(
            @PathVariable UUID userId,
            @Valid @RequestBody CreateWebhookRequest request) {

        userService.getUser(userId); // 404 if user not exist
        Webhook webhook = webhookService.createWebhook(userId, request.getUrl());
        return ResponseEntity.status(HttpStatus.CREATED).body(webhook);
    }

    @GetMapping("/{userId}/webhooks")
    public ResponseEntity<List<Webhook>> getUserWebhooks(@PathVariable UUID userId) {
        userService.getUser(userId);
        List<Webhook> webhooks = webhookService.getWebhooksByUserId(userId);
        return ResponseEntity.ok(webhooks);
    }

    @GetMapping("/{userId}/webhooks/{webhookId}")
    public ResponseEntity<Webhook> getWebhook(
            @PathVariable UUID userId,
            @PathVariable UUID webhookId) {
        userService.getUser(userId);
        Webhook webhook = webhookService.getWebhookById(webhookId);
        if (!webhook.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This webhook belongs to another user");
        }
        return ResponseEntity.ok(webhook);
    }

    @PutMapping("/{userId}/webhooks/{webhookId}")
    public ResponseEntity<Webhook> updateWebhook(
            @PathVariable UUID userId,
            @PathVariable UUID webhookId,
            @Valid @RequestBody UpdateWebhookRequest request) {
        userService.getUser(userId);
        Webhook webhook = webhookService.getWebhookById(webhookId);
        if (!webhook.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        Webhook updated = webhookService.updateWebhook(webhookId, request.getUrl());
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{userId}/webhooks/{webhookId}")
    public ResponseEntity<Void> deleteWebhook(
            @PathVariable UUID userId,
            @PathVariable UUID webhookId) {
        userService.getUser(userId);
        Webhook webhook = webhookService.getWebhookById(webhookId);
        if (!webhook.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        webhookService.deleteWebhook(webhookId);
        return ResponseEntity.noContent().build();
    }

}
