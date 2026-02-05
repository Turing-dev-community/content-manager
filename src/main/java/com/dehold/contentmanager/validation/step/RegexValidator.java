package com.dehold.contentmanager.validation.step;

import com.dehold.contentmanager.content.Content;
import com.dehold.contentmanager.validation.model.ValidationError;
import com.dehold.contentmanager.validation.model.ValidationResult;

import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validation step that checks an arbitrary field against a provided regular expression.
 * If the regex finds a match, validation fails (forbidden match). If no matches are found,
 * validation is successful.
 *
 * @param <T> content type
 */
public class RegexValidator<T extends Content> implements ValidationStep<T> {

    private final Function<T, String> getter;
    private final String fieldName;
    private final Pattern pattern;
    public static final String ERROR_CODE = "REGEX_VALIDATION_FAILED";

    public RegexValidator(Function<T, String> getter, String fieldName, String regex) {
        this.getter = getter;
        this.fieldName = fieldName;
        this.pattern = Pattern.compile(regex, Pattern.MULTILINE);
    }

    @Override
    public ValidationResult validate(T content) {
        String value = getter.apply(content);
        if (value == null) {
            // treat null as valid (no match)
            return ValidationResult.valid(content.getClass().getSimpleName(),
                    content.getId(), content.getUserId());
        }

        Matcher matcher = pattern.matcher(value);
        if (matcher.find()) {
            ValidationError err = new ValidationError(ERROR_CODE, errorMessageRegexMatch(fieldName, pattern.pattern()));
            return ValidationResult.invalid(content.getClass().getSimpleName(),
                    content.getId(), content.getUserId(), List.of(err));
        }

        return ValidationResult.valid(content.getClass().getSimpleName(),
                content.getId(), content.getUserId());
    }

    @Override
    public String getFieldName() {
        return this.fieldName;
    }

    public static String errorMessageRegexMatch(String fieldName, String regex) {
        return "The field '" + fieldName + "' matches forbidden pattern: " + regex;
    }
}