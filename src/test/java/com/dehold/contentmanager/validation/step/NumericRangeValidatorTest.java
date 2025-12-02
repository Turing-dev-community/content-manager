package com.dehold.contentmanager.validation.step;

import com.dehold.contentmanager.content.blogpost.model.BlogPost;
import com.dehold.contentmanager.validation.model.ValidationResult;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class NumericRangeValidatorTest {

    @Test
    void givenValueWithinRange_whenValidate_thenReturnValidResult() {
        BlogPost blogPost = new BlogPost(UUID.randomUUID(), "Title", "50", Instant.now(), Instant.now(), UUID.randomUUID());
        Function<BlogPost, String> getter = BlogPost::getContent;
        NumericRangeValidator<BlogPost> validator = new NumericRangeValidator<>(getter, "content", 0.0, 100.0);

        ValidationResult result = validator.validate(blogPost);

        assertTrue(result.isValid());
        assertEquals(0, result.getErrors().size());
    }

    @Test
    void givenValueBelowMinimum_whenValidate_thenReturnInvalidResult() {
        BlogPost blogPost = new BlogPost(UUID.randomUUID(), "Title", "-5", Instant.now(), Instant.now(), UUID.randomUUID());
        Function<BlogPost, String> getter = BlogPost::getContent;
        NumericRangeValidator<BlogPost> validator = new NumericRangeValidator<>(getter, "content", 0.0, 100.0);

        ValidationResult result = validator.validate(blogPost);

        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
        assertEquals(NumericRangeValidator.ERROR_CODE, result.getErrors().getFirst().code());
        assertEquals("The field 'content' must be greater than 0.0.", result.getErrors().getFirst().message());
    }

    @Test
    void givenValueAboveMaximum_whenValidate_thenReturnInvalidResult() {
        BlogPost blogPost = new BlogPost(UUID.randomUUID(), "Title", "150", Instant.now(), Instant.now(), UUID.randomUUID());
        Function<BlogPost, String> getter = BlogPost::getContent;
        NumericRangeValidator<BlogPost> validator = new NumericRangeValidator<>(getter, "content", 0.0, 100.0);

        ValidationResult result = validator.validate(blogPost);

        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
        assertEquals(NumericRangeValidator.ERROR_CODE, result.getErrors().getFirst().code());
        assertEquals("The field 'content' must be less than 100.0.", result.getErrors().getFirst().message());
    }

    @Test
    void givenDecimalValueWithinRange_whenValidate_thenReturnValidResult() {
        BlogPost blogPost = new BlogPost(UUID.randomUUID(), "Title", "45.5", Instant.now(), Instant.now(), UUID.randomUUID());
        Function<BlogPost, String> getter = BlogPost::getContent;
        NumericRangeValidator<BlogPost> validator = new NumericRangeValidator<>(getter, "content", 0.0, 100.0);

        ValidationResult result = validator.validate(blogPost);

        assertTrue(result.isValid());
        assertEquals(0, result.getErrors().size());
    }

    @Test
    void givenDecimalValueBelowMinimum_whenValidate_thenReturnInvalidResult() {
        BlogPost blogPost = new BlogPost(UUID.randomUUID(), "Title", "2.5", Instant.now(), Instant.now(), UUID.randomUUID());
        Function<BlogPost, String> getter = BlogPost::getContent;
        NumericRangeValidator<BlogPost> validator = new NumericRangeValidator<>(getter, "content", 5.0, 100.0);

        ValidationResult result = validator.validate(blogPost);

        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
        assertEquals(NumericRangeValidator.ERROR_CODE, result.getErrors().getFirst().code());
        assertEquals("The field 'content' must be greater than 5.0.", result.getErrors().getFirst().message());
    }

    @Test
    void givenDecimalValueAboveMaximum_whenValidate_thenReturnInvalidResult() {
        BlogPost blogPost = new BlogPost(UUID.randomUUID(), "Title", "105.7", Instant.now(), Instant.now(), UUID.randomUUID());
        Function<BlogPost, String> getter = BlogPost::getContent;
        NumericRangeValidator<BlogPost> validator = new NumericRangeValidator<>(getter, "content", 0.0, 100.0);

        ValidationResult result = validator.validate(blogPost);

        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
        assertEquals(NumericRangeValidator.ERROR_CODE, result.getErrors().getFirst().code());
        assertEquals("The field 'content' must be less than 100.0.", result.getErrors().getFirst().message());
    }

    @Test
    void givenNonNumericValue_whenValidate_thenReturnInvalidResult() {
        BlogPost blogPost = new BlogPost(UUID.randomUUID(), "Title", "not a number", Instant.now(), Instant.now(), UUID.randomUUID());
        Function<BlogPost, String> getter = BlogPost::getContent;
        NumericRangeValidator<BlogPost> validator = new NumericRangeValidator<>(getter, "content", 0.0, 100.0);

        ValidationResult result = validator.validate(blogPost);

        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
        assertEquals(NumericRangeValidator.ERROR_CODE, result.getErrors().getFirst().code());
        assertEquals("The field 'content' must be a valid number.", result.getErrors().getFirst().message());
    }

    @Test
    void givenNullValue_whenValidate_thenReturnValidResult() {
        BlogPost blogPost = new BlogPost(UUID.randomUUID(), "Title", null, Instant.now(), Instant.now(), UUID.randomUUID());
        Function<BlogPost, String> getter = BlogPost::getContent;
        NumericRangeValidator<BlogPost> validator = new NumericRangeValidator<>(getter, "content", 0.0, 100.0);

        ValidationResult result = validator.validate(blogPost);

        assertTrue(result.isValid());
        assertEquals(0, result.getErrors().size());
    }

    @Test
    void givenEmptyValue_whenValidate_thenReturnValidResult() {
        BlogPost blogPost = new BlogPost(UUID.randomUUID(), "Title", "", Instant.now(), Instant.now(), UUID.randomUUID());
        Function<BlogPost, String> getter = BlogPost::getContent;
        NumericRangeValidator<BlogPost> validator = new NumericRangeValidator<>(getter, "content", 0.0, 100.0);

        ValidationResult result = validator.validate(blogPost);

        assertTrue(result.isValid());
        assertEquals(0, result.getErrors().size());
    }

    @Test
    void givenValueWithOnlyMinimum_whenValidate_thenReturnValidResult() {
        BlogPost blogPost = new BlogPost(UUID.randomUUID(), "Title", "50", Instant.now(), Instant.now(), UUID.randomUUID());
        Function<BlogPost, String> getter = BlogPost::getContent;
        NumericRangeValidator<BlogPost> validator = new NumericRangeValidator<>(getter, "content", 10.0, null);

        ValidationResult result = validator.validate(blogPost);

        assertTrue(result.isValid());
        assertEquals(0, result.getErrors().size());
    }

    @Test
    void givenValueWithOnlyMaximum_whenValidate_thenReturnValidResult() {
        BlogPost blogPost = new BlogPost(UUID.randomUUID(), "Title", "50", Instant.now(), Instant.now(), UUID.randomUUID());
        Function<BlogPost, String> getter = BlogPost::getContent;
        NumericRangeValidator<BlogPost> validator = new NumericRangeValidator<>(getter, "content", null, 100.0);

        ValidationResult result = validator.validate(blogPost);

        assertTrue(result.isValid());
        assertEquals(0, result.getErrors().size());
    }

    @Test
    void givenValueBelowMinimumWithNoMaximum_whenValidate_thenReturnInvalidResult() {
        BlogPost blogPost = new BlogPost(UUID.randomUUID(), "Title", "5", Instant.now(), Instant.now(), UUID.randomUUID());
        Function<BlogPost, String> getter = BlogPost::getContent;
        NumericRangeValidator<BlogPost> validator = new NumericRangeValidator<>(getter, "content", 10.0, null);

        ValidationResult result = validator.validate(blogPost);

        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
        assertEquals(NumericRangeValidator.ERROR_CODE, result.getErrors().getFirst().code());
        assertEquals("The field 'content' must be greater than 10.0.", result.getErrors().getFirst().message());
    }

    @Test
    void givenValueAboveMaximumWithNoMinimum_whenValidate_thenReturnInvalidResult() {
        BlogPost blogPost = new BlogPost(UUID.randomUUID(), "Title", "150", Instant.now(), Instant.now(), UUID.randomUUID());
        Function<BlogPost, String> getter = BlogPost::getContent;
        NumericRangeValidator<BlogPost> validator = new NumericRangeValidator<>(getter, "content", null, 100.0);

        ValidationResult result = validator.validate(blogPost);

        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
        assertEquals(NumericRangeValidator.ERROR_CODE, result.getErrors().getFirst().code());
        assertEquals("The field 'content' must be less than 100.0.", result.getErrors().getFirst().message());
    }

    @Test
    void givenValueAtMinimumBoundary_whenValidate_thenReturnInvalidResult() {
        BlogPost blogPost = new BlogPost(UUID.randomUUID(), "Title", "0", Instant.now(), Instant.now(), UUID.randomUUID());
        Function<BlogPost, String> getter = BlogPost::getContent;
        NumericRangeValidator<BlogPost> validator = new NumericRangeValidator<>(getter, "content", 0.0, 100.0);

        ValidationResult result = validator.validate(blogPost);

        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
        assertEquals(NumericRangeValidator.ERROR_CODE, result.getErrors().getFirst().code());
        assertEquals("The field 'content' must be greater than 0.0.", result.getErrors().getFirst().message());
    }

    @Test
    void givenValueAtMaximumBoundary_whenValidate_thenReturnInvalidResult() {
        BlogPost blogPost = new BlogPost(UUID.randomUUID(), "Title", "100", Instant.now(), Instant.now(), UUID.randomUUID());
        Function<BlogPost, String> getter = BlogPost::getContent;
        NumericRangeValidator<BlogPost> validator = new NumericRangeValidator<>(getter, "content", 0.0, 100.0);

        ValidationResult result = validator.validate(blogPost);

        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
        assertEquals(NumericRangeValidator.ERROR_CODE, result.getErrors().getFirst().code());
        assertEquals("The field 'content' must be less than 100.0.", result.getErrors().getFirst().message());
    }

    @Test
    void givenNegativeValueWithinNegativeRange_whenValidate_thenReturnValidResult() {
        BlogPost blogPost = new BlogPost(UUID.randomUUID(), "Title", "-50", Instant.now(), Instant.now(), UUID.randomUUID());
        Function<BlogPost, String> getter = BlogPost::getContent;
        NumericRangeValidator<BlogPost> validator = new NumericRangeValidator<>(getter, "content", -100.0, -10.0);

        ValidationResult result = validator.validate(blogPost);

        assertTrue(result.isValid());
        assertEquals(0, result.getErrors().size());
    }

    @Test
    void getFieldName_shouldReturnCorrectFieldName() {
        Function<BlogPost, String> getter = BlogPost::getContent;
        NumericRangeValidator<BlogPost> validator = new NumericRangeValidator<>(getter, "testField", 0.0, 100.0);

        String fieldName = validator.getFieldName();

        assertEquals("testField", fieldName);
    }

    @Test
    void givenValueAtMinimumBoundaryWithInclusiveMin_whenValidate_thenReturnValidResult() {
        BlogPost blogPost = new BlogPost(UUID.randomUUID(), "Title", "0", Instant.now(), Instant.now(), UUID.randomUUID());
        Function<BlogPost, String> getter = BlogPost::getContent;
        NumericRangeValidator<BlogPost> validator = new NumericRangeValidator<>(getter, "content", 0.0, 100.0, true, false);

        ValidationResult result = validator.validate(blogPost);

        assertTrue(result.isValid());
        assertEquals(0, result.getErrors().size());
    }

    @Test
    void givenValueAtMaximumBoundaryWithInclusiveMax_whenValidate_thenReturnValidResult() {
        BlogPost blogPost = new BlogPost(UUID.randomUUID(), "Title", "100", Instant.now(), Instant.now(), UUID.randomUUID());
        Function<BlogPost, String> getter = BlogPost::getContent;
        NumericRangeValidator<BlogPost> validator = new NumericRangeValidator<>(getter, "content", 0.0, 100.0, false, true);

        ValidationResult result = validator.validate(blogPost);

        assertTrue(result.isValid());
        assertEquals(0, result.getErrors().size());
    }

    @Test
    void givenValueAtBothBoundariesWithBothInclusive_whenValidate_thenReturnValidResult() {
        BlogPost blogPost1 = new BlogPost(UUID.randomUUID(), "Title", "0", Instant.now(), Instant.now(), UUID.randomUUID());
        BlogPost blogPost2 = new BlogPost(UUID.randomUUID(), "Title", "100", Instant.now(), Instant.now(), UUID.randomUUID());
        Function<BlogPost, String> getter = BlogPost::getContent;
        NumericRangeValidator<BlogPost> validator = new NumericRangeValidator<>(getter, "content", 0.0, 100.0, true, true);

        ValidationResult result1 = validator.validate(blogPost1);
        ValidationResult result2 = validator.validate(blogPost2);

        assertTrue(result1.isValid());
        assertTrue(result2.isValid());
    }

    @Test
    void givenValueBelowInclusiveMinimum_whenValidate_thenReturnInvalidResult() {
        BlogPost blogPost = new BlogPost(UUID.randomUUID(), "Title", "-0.1", Instant.now(), Instant.now(), UUID.randomUUID());
        Function<BlogPost, String> getter = BlogPost::getContent;
        NumericRangeValidator<BlogPost> validator = new NumericRangeValidator<>(getter, "content", 0.0, 100.0, true, false);

        ValidationResult result = validator.validate(blogPost);

        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
        assertEquals(NumericRangeValidator.ERROR_CODE, result.getErrors().getFirst().code());
        assertEquals("The field 'content' must be at least 0.0.", result.getErrors().getFirst().message());
    }

    @Test
    void givenValueAboveInclusiveMaximum_whenValidate_thenReturnInvalidResult() {
        BlogPost blogPost = new BlogPost(UUID.randomUUID(), "Title", "100.1", Instant.now(), Instant.now(), UUID.randomUUID());
        Function<BlogPost, String> getter = BlogPost::getContent;
        NumericRangeValidator<BlogPost> validator = new NumericRangeValidator<>(getter, "content", 0.0, 100.0, false, true);

        ValidationResult result = validator.validate(blogPost);

        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
        assertEquals(NumericRangeValidator.ERROR_CODE, result.getErrors().getFirst().code());
        assertEquals("The field 'content' must be at most 100.0.", result.getErrors().getFirst().message());
    }

    @Test
    void givenValueAtMinimumWithExclusiveMin_whenValidate_thenReturnInvalidResult() {
        BlogPost blogPost = new BlogPost(UUID.randomUUID(), "Title", "10", Instant.now(), Instant.now(), UUID.randomUUID());
        Function<BlogPost, String> getter = BlogPost::getContent;
        NumericRangeValidator<BlogPost> validator = new NumericRangeValidator<>(getter, "content", 10.0, 100.0, false, true);

        ValidationResult result = validator.validate(blogPost);

        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
        assertEquals(NumericRangeValidator.ERROR_CODE, result.getErrors().getFirst().code());
        assertEquals("The field 'content' must be greater than 10.0.", result.getErrors().getFirst().message());
    }

    @Test
    void givenValueAtMaximumWithExclusiveMax_whenValidate_thenReturnInvalidResult() {
        BlogPost blogPost = new BlogPost(UUID.randomUUID(), "Title", "100", Instant.now(), Instant.now(), UUID.randomUUID());
        Function<BlogPost, String> getter = BlogPost::getContent;
        NumericRangeValidator<BlogPost> validator = new NumericRangeValidator<>(getter, "content", 0.0, 100.0, true, false);

        ValidationResult result = validator.validate(blogPost);

        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
        assertEquals(NumericRangeValidator.ERROR_CODE, result.getErrors().getFirst().code());
        assertEquals("The field 'content' must be less than 100.0.", result.getErrors().getFirst().message());
    }

    @Test
    void givenValueWithinInclusiveRange_whenValidate_thenReturnValidResult() {
        BlogPost blogPost = new BlogPost(UUID.randomUUID(), "Title", "50", Instant.now(), Instant.now(), UUID.randomUUID());
        Function<BlogPost, String> getter = BlogPost::getContent;
        NumericRangeValidator<BlogPost> validator = new NumericRangeValidator<>(getter, "content", 0.0, 100.0, true, true);

        ValidationResult result = validator.validate(blogPost);

        assertTrue(result.isValid());
        assertEquals(0, result.getErrors().size());
    }

    @Test
    void givenDecimalValueAtInclusiveMinimum_whenValidate_thenReturnValidResult() {
        BlogPost blogPost = new BlogPost(UUID.randomUUID(), "Title", "5.5", Instant.now(), Instant.now(), UUID.randomUUID());
        Function<BlogPost, String> getter = BlogPost::getContent;
        NumericRangeValidator<BlogPost> validator = new NumericRangeValidator<>(getter, "content", 5.5, 100.0, true, false);

        ValidationResult result = validator.validate(blogPost);

        assertTrue(result.isValid());
        assertEquals(0, result.getErrors().size());
    }

    @Test
    void givenDecimalValueAtInclusiveMaximum_whenValidate_thenReturnValidResult() {
        BlogPost blogPost = new BlogPost(UUID.randomUUID(), "Title", "99.9", Instant.now(), Instant.now(), UUID.randomUUID());
        Function<BlogPost, String> getter = BlogPost::getContent;
        NumericRangeValidator<BlogPost> validator = new NumericRangeValidator<>(getter, "content", 0.0, 99.9, false, true);

        ValidationResult result = validator.validate(blogPost);

        assertTrue(result.isValid());
        assertEquals(0, result.getErrors().size());
    }

    @Test
    void givenNegativeValueAtInclusiveMinimum_whenValidate_thenReturnValidResult() {
        BlogPost blogPost = new BlogPost(UUID.randomUUID(), "Title", "-100", Instant.now(), Instant.now(), UUID.randomUUID());
        Function<BlogPost, String> getter = BlogPost::getContent;
        NumericRangeValidator<BlogPost> validator = new NumericRangeValidator<>(getter, "content", -100.0, -10.0, true, true);

        ValidationResult result = validator.validate(blogPost);

        assertTrue(result.isValid());
        assertEquals(0, result.getErrors().size());
    }

    @Test
    void givenMixedBoundariesInclusiveMinExclusiveMax_whenValidate_thenBehavesCorrectly() {
        Function<BlogPost, String> getter = BlogPost::getContent;
        NumericRangeValidator<BlogPost> validator = new NumericRangeValidator<>(getter, "content", 0.0, 100.0, true, false);

        BlogPost atMin = new BlogPost(UUID.randomUUID(), "Title", "0", Instant.now(), Instant.now(), UUID.randomUUID());
        BlogPost atMax = new BlogPost(UUID.randomUUID(), "Title", "100", Instant.now(), Instant.now(), UUID.randomUUID());
        BlogPost inRange = new BlogPost(UUID.randomUUID(), "Title", "50", Instant.now(), Instant.now(), UUID.randomUUID());

        assertTrue(validator.validate(atMin).isValid());
        assertFalse(validator.validate(atMax).isValid());
        assertTrue(validator.validate(inRange).isValid());
    }

    @Test
    void givenMixedBoundariesExclusiveMinInclusiveMax_whenValidate_thenBehavesCorrectly() {
        Function<BlogPost, String> getter = BlogPost::getContent;
        NumericRangeValidator<BlogPost> validator = new NumericRangeValidator<>(getter, "content", 0.0, 100.0, false, true);

        BlogPost atMin = new BlogPost(UUID.randomUUID(), "Title", "0", Instant.now(), Instant.now(), UUID.randomUUID());
        BlogPost atMax = new BlogPost(UUID.randomUUID(), "Title", "100", Instant.now(), Instant.now(), UUID.randomUUID());
        BlogPost inRange = new BlogPost(UUID.randomUUID(), "Title", "50", Instant.now(), Instant.now(), UUID.randomUUID());

        assertFalse(validator.validate(atMin).isValid());
        assertTrue(validator.validate(atMax).isValid());
        assertTrue(validator.validate(inRange).isValid());
    }
}