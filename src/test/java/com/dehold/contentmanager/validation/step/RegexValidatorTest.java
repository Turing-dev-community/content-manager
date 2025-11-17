package com.dehold.contentmanager.validation.step;

import com.dehold.contentmanager.content.customersupport.model.SupportRequest;
import com.dehold.contentmanager.validation.model.ValidationResult;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class RegexValidatorTest {

    // Regex forbidding words 'spam' or 'badword' (case-insensitive)
    private final RegexValidator<SupportRequest> validator =
            new RegexValidator<>(SupportRequest::getText, "text", ".*\\b(?:spam|badword)\\b.*");

    @Test
    void validate_whenNoMatch_shouldReturnValidResult() {
        SupportRequest req = new SupportRequest();
        req.setText("This is a perfectly fine message without forbidden tokens.");
        req.setId(UUID.randomUUID());
        req.setUserId(UUID.randomUUID());
        req.setCreatedAt(Instant.now());
        req.setUpdatedAt(Instant.now());

        ValidationResult result = validator.validate(req);
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void validate_whenRegexMatches_shouldReturnInvalidResult() {
        SupportRequest req = new SupportRequest();
        req.setText("This message contains spam which should be caught.");
        req.setId(UUID.randomUUID());
        req.setUserId(UUID.randomUUID());
        req.setCreatedAt(Instant.now());
        req.setUpdatedAt(Instant.now());

        ValidationResult result = validator.validate(req);
        assertFalse(result.isValid());
        assertFalse(result.getErrors().isEmpty());
        assertEquals(1, result.getErrors().size());
        assertEquals(RegexValidator.ERROR_CODE, result.getErrors().get(0).code());
    }

    @Test
    void validate_whenRegexMatchesWithDifferentCase_shouldReturnInvalidResult() {
        SupportRequest req = new SupportRequest();
        // Uppercase version — tests case-insensitive behavior
        req.setText("This message contains SPAM in uppercase.");
        req.setId(UUID.randomUUID());
        req.setUserId(UUID.randomUUID());
        req.setCreatedAt(Instant.now());
        req.setUpdatedAt(Instant.now());

        ValidationResult result = validator.validate(req);
        assertFalse(result.isValid());
        assertFalse(result.getErrors().isEmpty());
        assertEquals(1, result.getErrors().size());
        assertEquals(RegexValidator.ERROR_CODE, result.getErrors().get(0).code());
    }

    @Test
    void errorMessage_containsFieldNameAndPattern() {
        String msg = RegexValidator.errorMessageRegexMatch("myField", "foo|bar");
        assertNotNull(msg);
        assertTrue(msg.contains("myField"));
        assertTrue(msg.contains("foo|bar"));
    }
}