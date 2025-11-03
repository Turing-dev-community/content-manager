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
        Function<BlogPost, String> getter = BlogPost::getContent;
        cut = new ForbiddenWordValidator<BlogPost>(forbiddenWordsService, getter, "content", UUID.randomUUID());
        List<ForbiddenWords> forbiddenWordsList = List.of(defaultForbiddenWords, customForbiddenWords);
        when(forbiddenWordsService.findByUserId(any(UUID.class))).thenReturn(forbiddenWordsList);
    }

    @Test
    void givenBlogPostWithNoForbiddenWords_whenValidate_thenReturnValidResult() {
        BlogPost blogPost = new BlogPost(UUID.randomUUID(),"Some Title", "This is a clean blog post content.",
                Instant.now(), Instant.now(), UUID.randomUUID());
        ValidationResult result = cut.validate(blogPost);
        assertTrue(result.isValid());
    }

    @Test
    void givenBlogPostWithDefaultForbiddenWord_whenValidate_thenReturnInvalidResult() {
        BlogPost blogPost = new BlogPost(UUID.randomUUID(),"Some Title", "This content contains badword1.",
                Instant.now(), Instant.now(), UUID.randomUUID());
        ValidationResult result = cut.validate(blogPost);
        assertFalse(result.isValid());
    }

    @Test
    void givenBlogPostWithCustomForbiddenWord_whenValidate_thenReturnInvalidResult() {
        BlogPost blogPost = new BlogPost(UUID.randomUUID(),"Some Title", "This content contains custombadword1.",
                Instant.now(), Instant.now(), UUID.randomUUID());
        ValidationResult result = cut.validate(blogPost);
        assertFalse(result.isValid());
    }

}