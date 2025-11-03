package com.dehold.contentmanager.validation.step;

import com.dehold.contentmanager.content.Content;
import com.dehold.contentmanager.validation.model.ForbiddenWords;
import com.dehold.contentmanager.validation.model.ValidationError;
import com.dehold.contentmanager.validation.model.ValidationResult;
import com.dehold.contentmanager.validation.service.ForbiddenWordsService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ForbiddenWordValidator<T extends Content> implements ValidationStep<T> {

    private final ForbiddenWordsService service;
    private final Function<T, String> getter;
    private final String fieldName;
    private final UUID userId;
    public static final String ERROR_CODE = "FORBIDDEN_WORD_VALIDATION_FAILED";

    public ForbiddenWordValidator(ForbiddenWordsService service, Function<T, String> getter, String fieldName,
                                  UUID userId) {
        this.service = service;
        this.getter = getter;
        this.fieldName = fieldName;
        this.userId = userId;
    }


    @Override
    public ValidationResult validate(T content) {
        List<ForbiddenWords> forbiddenWordsList = service.findByUserId(content.getUserId());
        Set<String> forbiddenWords =
                forbiddenWordsList.stream().map(ForbiddenWords::getWords).flatMap(Set::stream).collect(Collectors.toSet());
        String value = getter.apply(content);
        for(String word : forbiddenWords) {
            if(value != null && value.contains(word)) {
                return ValidationResult.invalid(content.getClass().getSimpleName(),
                        content.getId(),
                        content.getUserId(),
                        List.of(new ValidationError(
                                ERROR_CODE,
                                errorMessage(fieldName, word)
                        )));
            }
        }
        return ValidationResult.valid(content.getClass().getSimpleName(), content.getId(), content.getUserId());
    }

    public String errorMessage(String fieldName, String forbiddenWord) {
        return "The field '" + fieldName + "' contains the forbidden word: '" + forbiddenWord + "'";
    }

    @Override
    public String getFieldName() {
        return fieldName;
    }

    public UUID getUserId() {
        return userId;
    }
}
