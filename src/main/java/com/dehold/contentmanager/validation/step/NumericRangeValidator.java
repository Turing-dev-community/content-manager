package com.dehold.contentmanager.validation.step;

import com.dehold.contentmanager.content.Content;
import com.dehold.contentmanager.validation.model.ValidationError;
import com.dehold.contentmanager.validation.model.ValidationResult;

import java.util.List;
import java.util.function.Function;

public class NumericRangeValidator<T extends Content> implements ValidationStep<T> {

    private final Function<T, String> getter;
    private final String fieldName;
    private final Double minValue;
    private final Double maxValue;
    private final boolean minInclusive;
    private final boolean maxInclusive;
    public static final String ERROR_CODE = "NUMERIC_RANGE_VALIDATION_FAILED";

    public NumericRangeValidator(Function<T, String> getter, String fieldName, Double minValue, Double maxValue, boolean minInclusive, boolean maxInclusive) {
        this.getter = getter;
        this.fieldName = fieldName;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.minInclusive = minInclusive;
        this.maxInclusive = maxInclusive;
    }

    public NumericRangeValidator(Function<T, String> getter, String fieldName, Double minValue, Double maxValue) {
        this(getter, fieldName, minValue, maxValue, false, false);
    }

    @Override
    public ValidationResult validate(T content) {
        String value = getter.apply(content);

        if (value == null || value.isEmpty()) {
            return ValidationResult.valid(content.getClass().getSimpleName(), content.getId(), content.getUserId());
        }

        try {
            double numericValue = Double.parseDouble(value);

            if (minValue != null) {
                boolean violatesMin = minInclusive ? numericValue < minValue : numericValue <= minValue;
                if (violatesMin) {
                    return ValidationResult.invalid(
                            content.getClass().getSimpleName(),
                            content.getId(),
                            content.getUserId(),
                            List.of(new ValidationError(ERROR_CODE, errorMessageTooSmall(fieldName, minValue, minInclusive)))
                    );
                }
            }

            if (maxValue != null) {
                boolean violatesMax = maxInclusive ? numericValue > maxValue : numericValue >= maxValue;
                if (violatesMax) {
                    return ValidationResult.invalid(
                            content.getClass().getSimpleName(),
                            content.getId(),
                            content.getUserId(),
                            List.of(new ValidationError(ERROR_CODE, errorMessageTooLarge(fieldName, maxValue, maxInclusive)))
                    );
                }
            }

            return ValidationResult.valid(content.getClass().getSimpleName(), content.getId(), content.getUserId());

        } catch (NumberFormatException e) {
            return ValidationResult.invalid(
                    content.getClass().getSimpleName(),
                    content.getId(),
                    content.getUserId(),
                    List.of(new ValidationError(ERROR_CODE, errorMessageNotNumeric(fieldName)))
            );
        }
    }

    @Override
    public String getFieldName() {
        return fieldName;
    }

    public static String errorMessageTooSmall(String fieldName, Double minValue, boolean inclusive) {
        String operator = inclusive ? "at least" : "greater than";
        return String.format("The field '%s' must be %s %s.", fieldName, operator, minValue);
    }

    public static String errorMessageTooLarge(String fieldName, Double maxValue, boolean inclusive) {
        String operator = inclusive ? "at most" : "less than";
        return String.format("The field '%s' must be %s %s.", fieldName, operator, maxValue);
    }

    public static String errorMessageNotNumeric(String fieldName) {
        return String.format("The field '%s' must be a valid number.", fieldName);
    }
}
