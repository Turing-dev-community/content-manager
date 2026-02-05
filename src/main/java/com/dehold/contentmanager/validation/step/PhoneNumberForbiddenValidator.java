package com.dehold.contentmanager.validation.step;

import com.dehold.contentmanager.content.Content;
import com.dehold.contentmanager.validation.model.ValidationError;
import com.dehold.contentmanager.validation.model.ValidationResult;

import java.util.List;
import java.util.function.Function;

public class PhoneNumberForbiddenValidator<T extends Content> implements ValidationStep<T>{
    private final Function<T, String> getter;
    private final String fieldName;
    public static final String ERROR_CODE = "PHONE_NUMBER_FORBIDDEN_VALIDATION_FAILED";

    private static final String PHONE_PATTERN = ".*\\b\\d{7}\\b.*";

    public PhoneNumberForbiddenValidator(Function<T, String> getter, String fieldName) {
        this.getter = getter;
        this.fieldName = fieldName;
    }

    @Override
    public ValidationResult validate(T content) {
        String value = getter.apply(content);
        if (value != null && value.matches(PHONE_PATTERN)) {
            return ValidationResult.invalid(content.getClass().getSimpleName(),
                    content.getId(), content.getUserId(), List.of(new ValidationError(ERROR_CODE,
                    errorMessagePhoneNumberForbidden(fieldName))));
        }
        return ValidationResult.valid(content.getClass().getSimpleName(),
                content.getId(), content.getUserId());
    }

    @Override
    public String getFieldName() {
        return this.fieldName;
    }

    public static String errorMessagePhoneNumberForbidden(String fieldName) {
        return "The field '" + fieldName + "' contains a phone number, which is not allowed.";
    }
}
