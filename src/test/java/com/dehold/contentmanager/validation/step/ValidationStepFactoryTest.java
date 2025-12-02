package com.dehold.contentmanager.validation.step;

import com.dehold.contentmanager.validation.model.ValidationStepType;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ValidationStepFactoryTest {

    @Test
    void givenValidationStepTypeLengthValidation_whenCreateValidationStep_thenReturnLengthValidator() {
        ValidationStepFactory factory = new ValidationStepFactory(null);

        ValidationStep<?> step = factory.createValidationStep(
                ValidationStepType.LENGTH_VALIDATION,
                content -> "test",
                Map.of("minLength", "5", "maxLength", "10"),
                "testField",
                UUID.randomUUID()
        );

        assertInstanceOf(LengthValidator.class, step);
        assertEquals("testField", step.getFieldName());
        LengthValidator<?> lengthValidator = (LengthValidator<?>) step;
        assertEquals(5, lengthValidator.getMinLength());
        assertEquals(10, lengthValidator.getMaxLength());
    }

    @Test
    void givenValidationStepTypeForbiddenWordValidation_whenCreateValidationStep_thenReturnForbiddenWordValidator() {
        ValidationStepFactory factory = new ValidationStepFactory(null);

        ValidationStep<?> step = factory.createValidationStep(
                ValidationStepType.FORBIDDEN_WORD_VALIDATION,
                content -> "test",
                Map.of(),
                "testField",
                UUID.randomUUID()
        );

        assertInstanceOf(ForbiddenWordValidator.class, step);
        assertEquals("testField", step.getFieldName());
    }

    @Test
    void givenValidationStepTypePhoneNumberForbiddenValidation_whenCreateValidationStep_thenReturnPhoneNumberForbiddenValidator() {
        ValidationStepFactory factory = new ValidationStepFactory(null);

        ValidationStep<?> step = factory.createValidationStep(
                ValidationStepType.PHONE_NUMBER_FORBIDDEN_VALIDATION,
                content -> "test",
                Map.of(),
                "testField",
                UUID.randomUUID()
        );

        assertInstanceOf(PhoneNumberForbiddenValidator.class, step);
        assertEquals("testField", step.getFieldName());
    }

    @Test
    void givenValidationStepTypeRegexValidation_whenCreateValidationStep_thenReturnRegexValidator() {
        ValidationStepFactory factory = new ValidationStepFactory(null); 
        ValidationStep<?> step = factory.createValidationStep(
            ValidationStepType.REGEX_VALIDATION,
            content -> "test content",
            Map.of("pattern", "\\bspam\\b"),  // simple regex pattern
            "content",
            UUID.randomUUID()
        );

        assertInstanceOf(RegexValidator.class, step);
        assertEquals("content", step.getFieldName());
    }
}