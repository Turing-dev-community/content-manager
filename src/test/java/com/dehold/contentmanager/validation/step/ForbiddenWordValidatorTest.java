package com.dehold.contentmanager.validation.step;

import com.dehold.contentmanager.content.blogpost.model.BlogPost;
import com.dehold.contentmanager.validation.model.ForbiddenWords;
import com.dehold.contentmanager.validation.model.ValidationResult;
import com.dehold.contentmanager.validation.service.ForbiddenWordsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ForbiddenWordValidatorTest {

    @Mock
    ForbiddenWordsService forbiddenWordsService;

    @InjectMocks
    ForbiddenWordValidator<BlogPost> cut;

    ForbiddenWords defaultForbiddenWords = new ForbiddenWords(null, null, "Default Forbidden Words", "any", "any",
            new java.util.LinkedHashSet<>(java.util.Arrays.asList("badword1", "badword2")));
    ForbiddenWords customForbiddenWords = new ForbiddenWords(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Custom Forbidden Words",
            "blogpost",
            "content",
            new java.util.LinkedHashSet<>(java.util.Arrays.asList("custombadword1", "custombadword2"))
    );

    @BeforeEach
    void setup() {
        List<ForbiddenWords> forbiddenWordsList = List.of(defaultForbiddenWords, customForbiddenWords);
        String contentType = BlogPost.class.getSimpleName().toLowerCase();
        when(forbiddenWordsService.findByUserIdAndContentType(any(UUID.class), eq(contentType))).thenReturn(forbiddenWordsList);
    }

    @Test
    void givenContentWithNoForbiddenWords_whenValidate_thenReturnValidResult() {
        BlogPost blogPost = new BlogPost(UUID.randomUUID(),"Some Title", "This is a clean blog post content.",
                Instant.now(), Instant.now(), UUID.randomUUID());
        Function<BlogPost, String> getter = BlogPost::getContent;
        ValidationStepFactory factory = new ValidationStepFactory(forbiddenWordsService);
        String fieldThatShouldBeValidated = "content";
        cut = factory.createForbiddenWordValidator(getter, fieldThatShouldBeValidated, UUID.randomUUID());

        ValidationResult result = cut.validate(blogPost);

        assertTrue(result.isValid());
    }

    @Test
    void givenContentWithDefaultForbiddenWord_whenValidate_thenReturnInvalidResult() {
        BlogPost blogPost = new BlogPost(UUID.randomUUID(),"Some Title", "This content contains badword1.",
                Instant.now(), Instant.now(), UUID.randomUUID());
        Function<BlogPost, String> getter = BlogPost::getContent;
        ValidationStepFactory factory = new ValidationStepFactory(forbiddenWordsService);
        String fieldThatShouldBeValidated = "content";
        cut = factory.createForbiddenWordValidator(getter, fieldThatShouldBeValidated, UUID.randomUUID());

        ValidationResult result = cut.validate(blogPost);

        assertFalse(result.isValid());
    }

    @Test
    void givenContentWithSeveralForbiddenWords_whenValidate_thenReturnOneErrorObject() {
        BlogPost blogPost = new BlogPost(UUID.randomUUID(),"Some Title", "This content contains badword1 and badword2.",
                Instant.now(), Instant.now(), UUID.randomUUID());
        Function<BlogPost, String> getter = BlogPost::getContent;
        ValidationStepFactory factory = new ValidationStepFactory(forbiddenWordsService);
        String fieldThatShouldBeValidated = "content";
        cut = factory.createForbiddenWordValidator(getter, fieldThatShouldBeValidated, UUID.randomUUID());

        ValidationResult result = cut.validate(blogPost);

        assertEquals(1, result.getErrors().size());
    }

    @Test
    void givenContentWithSeveralForbiddenWords_whenValidate_thenReturnErrorMessageWithAllWords() {
        BlogPost blogPost = new BlogPost(UUID.randomUUID(),"Some Title", "This content contains badword1 and badword2.",
                Instant.now(), Instant.now(), UUID.randomUUID());
        Function<BlogPost, String> getter = BlogPost::getContent;
        ValidationStepFactory factory = new ValidationStepFactory(forbiddenWordsService);
        String fieldThatShouldBeValidated = "content";
        cut = factory.createForbiddenWordValidator(getter, fieldThatShouldBeValidated, UUID.randomUUID());

        ValidationResult result = cut.validate(blogPost);

        assertTrue(result.getErrors().getFirst().message().contains("badword1"));
        assertTrue(result.getErrors().getFirst().message().contains("badword2"));
    }

    @Test
    void givenContentWithCustomForbiddenWord_whenValidate_thenReturnInvalidResult() {
        BlogPost blogPost = new BlogPost(UUID.randomUUID(),"Some Title", "This content contains custombadword1.",
                Instant.now(), Instant.now(), UUID.randomUUID());

        Function<BlogPost, String> getter = BlogPost::getContent;
        ValidationStepFactory factory = new ValidationStepFactory(forbiddenWordsService);
        String fieldThatShouldBeValidated = "content";
        cut = factory.createForbiddenWordValidator(getter, fieldThatShouldBeValidated, UUID.randomUUID());

        ValidationResult result = cut.validate(blogPost);

        assertFalse(result.isValid());
    }

    @Test
    void givenContentWithForbiddenWords_whenValidate_thenErrorCodeIsCorrect() {
        BlogPost blogPost = new BlogPost(UUID.randomUUID(), "Some Title", "This content contains badword1.",
                Instant.now(), Instant.now(), UUID.randomUUID());

        Function<BlogPost, String> getter = BlogPost::getContent;
        ValidationStepFactory factory = new ValidationStepFactory(forbiddenWordsService);
        String fieldThatShouldBeValidated = "content";
        cut = factory.createForbiddenWordValidator(getter, fieldThatShouldBeValidated, UUID.randomUUID());

        ValidationResult result = cut.validate(blogPost);

        String expected = ForbiddenWordValidator.ERROR_CODE;
        String actual = result.getErrors().getFirst().code();
        assertEquals(expected, actual);
    }

    @Test
    void givenContentWithForbiddenWords_whenValidate_thenErrorMessageIsCorrect() {
        BlogPost blogPost = new BlogPost(UUID.randomUUID(), "Some Title", "This content contains badword1.",
                Instant.now(), Instant.now(), UUID.randomUUID());

        Function<BlogPost, String> getter = BlogPost::getContent;
        ValidationStepFactory factory = new ValidationStepFactory(forbiddenWordsService);
        String fieldThatShouldBeValidated = "content";
        cut = factory.createForbiddenWordValidator(getter, fieldThatShouldBeValidated, UUID.randomUUID());

        ValidationResult result = cut.validate(blogPost);

        String expected = cut.errorMessage("content", "badword1");
        String actual = result.getErrors().getFirst().message();
        assertEquals(expected, actual);
    }

    @Test
    void givenFieldThatWasNotSpecifiedContainsForbiddenWords_whenValidate_thenReturnValidResult() {
        BlogPost blogPost = new BlogPost(UUID.randomUUID(), "Title with a badword1", "But actually only the content " +
                "is checked.",
                Instant.now(), Instant.now(), UUID.randomUUID());
        Function<BlogPost, String> getter = BlogPost::getContent;
        ValidationStepFactory factory = new ValidationStepFactory(forbiddenWordsService);
        String fieldThatShouldBeValidated = "content";
        cut = factory.createForbiddenWordValidator(getter, fieldThatShouldBeValidated, UUID.randomUUID());

        ValidationResult result = cut.validate(blogPost);

        assertTrue(result.isValid());
    }

    @Test
    void givenContent_whenValidated_thenValidationResultContainsGenericInfos() {
        BlogPost blogPost = new BlogPost(UUID.randomUUID(), "Some title", "Some content.",
                Instant.now(), Instant.now(), UUID.randomUUID());

        Function<BlogPost, String> getter = BlogPost::getContent;
        ValidationStepFactory factory = new ValidationStepFactory(forbiddenWordsService);
        String fieldThatShouldBeValidated = "content";
        cut = factory.createForbiddenWordValidator(getter, fieldThatShouldBeValidated, UUID.randomUUID());

        ValidationResult result = cut.validate(blogPost);

        assertEquals(result.getContentType(), blogPost.getClass().getSimpleName());
        assertEquals(result.getContentId(), blogPost.getId());
        assertEquals(result.getUserId(), blogPost.getUserId());
    }
}