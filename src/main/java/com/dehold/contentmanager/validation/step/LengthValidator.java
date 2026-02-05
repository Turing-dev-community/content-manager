package com.dehold.contentmanager.validation.step;

import com.dehold.contentmanager.content.Content;
import com.dehold.contentmanager.validation.model.ValidationError;
import com.dehold.contentmanager.validation.model.ValidationResult;

import java.util.List;
import java.util.function.Function;


public class LengthValidator<T extends Content> implements ValidationStep<T> {

    private final Function<T, String> getter;
    private final String fieldName;
    private final int minLength;
    private final int maxLength;
    public static final String ERROR_CODE = "LENGTH_VALIDATION_FAILED";

    public LengthValidator(Function<T, String> getter, String fieldName, int minLength, int maxLength) {
        this.getter = getter;
        this.fieldName = fieldName;
        this.minLength = minLength;
        this.maxLength = maxLength;
    }

    @Override
    public ValidationResult validate(T content) {
        String value = getter.apply(content);
        if (minLength < 0 || maxLength < 0 || minLength > maxLength) {
            throw new IllegalArgumentException("Invalid min/max lengths");
        }
        if(value.length() <= minLength) return ValidationResult.invalid(content.getClass().getSimpleName(),
                content.getId(),
                content.getUserId(),
                List.of(new ValidationError(ERROR_CODE,
                errorMessageTooShort(fieldName))));
        if(value.length() >= maxLength) return ValidationResult.invalid(content.getClass().getSimpleName(),
                content.getId(),content.getUserId(),List.of(new ValidationError(ERROR_CODE,
                errorMessageTooShort(fieldName))));
        return ValidationResult.valid(content.getClass().getSimpleName(),
                content.getId(), content.getUserId());
    }

    @Override
    public String getFieldName() {
        return this.fieldName;
    }

    public static String errorMessageTooShort(String fieldName) {
        return "The field '" + fieldName + "' is too short.";
    }

    public static String errorMessageTooLong(String fieldName) {
        return "The field '" + fieldName + "' is too long.";
    }

    public int getMinLength() {
        return minLength;
    }

    public int getMaxLength() {
        return maxLength;
    }
}
