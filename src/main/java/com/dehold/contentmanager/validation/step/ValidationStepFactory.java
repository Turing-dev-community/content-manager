package com.dehold.contentmanager.validation.step;

import com.dehold.contentmanager.content.Content;
import com.dehold.contentmanager.validation.model.ValidationStepModel;
import com.dehold.contentmanager.validation.model.ValidationStepType;
import com.dehold.contentmanager.validation.service.ForbiddenWordsService;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Component
public class ValidationStepFactory {

    private final ForbiddenWordsService forbiddenWordsService;

    public ValidationStepFactory(ForbiddenWordsService forbiddenWordsService) {
        this.forbiddenWordsService = forbiddenWordsService;
    }

    public <T extends Content> ValidationStep<T> createValidationStep(ValidationStepType type,
                                                                     Function<T, String> fieldExtractor, Map<String,
                    String> params, String fieldName, UUID userId) {
        return switch (type) {
            case FORBIDDEN_WORD_VALIDATION ->
                    createForbiddenWordValidator(fieldExtractor, fieldName, userId);
            case LENGTH_VALIDATION -> {
                int minLength = Integer.parseInt(params.get("minLength"));
                int maxLength = Integer.parseInt(params.get("maxLength"));
                yield createLengthValidator(fieldExtractor, fieldName, minLength, maxLength);
            }
            case PHONE_NUMBER_FORBIDDEN_VALIDATION ->
                    createPhoneNumberValidator(fieldExtractor, fieldName);
        };
    }

    private <T extends Content> ForbiddenWordValidator<T> createForbiddenWordValidator(
            Function<T, String> getter, String fieldName, UUID userId) {
        return new ForbiddenWordValidator<>(forbiddenWordsService, getter, fieldName, userId);
    }

    private <T extends Content> LengthValidator<T> createLengthValidator(
            Function<T, String> getter, String fieldName, int minLength, int maxLength) {
        return new LengthValidator<>(getter, fieldName, minLength, maxLength);
    }

    private <T extends Content> PhoneNumberForbiddenValidator<T> createPhoneNumberValidator(
            Function<T, String> getter, String fieldName) {
        return new PhoneNumberForbiddenValidator<>(getter, fieldName);
    }
}

