package com.dehold.contentmanager.validation.step;

import com.dehold.contentmanager.content.Content;
import com.dehold.contentmanager.validation.model.ForbiddenWords;
import com.dehold.contentmanager.validation.model.ValidationError;
import com.dehold.contentmanager.validation.model.ValidationResult;
import com.dehold.contentmanager.validation.service.ForbiddenWordsService;

import java.util.ArrayList;
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
        Set<String> forbiddenWords = fetchForbiddenWords(content);
        List<String> foundViolations = findViolations(content, forbiddenWords);
        if(!foundViolations.isEmpty()) {
            return buildInvalidValidationResult(content, foundViolations);
        }
        return ValidationResult.valid(content.getClass().getSimpleName(), content.getId(), content.getUserId());
    }

    private ValidationResult buildInvalidValidationResult(T content, List<String> foundViolations) {
        return ValidationResult.invalid(content.getClass().getSimpleName(),
                content.getId(),
                content.getUserId(),
                List.of(new ValidationError(
                        ERROR_CODE,
                        errorMessage(fieldName, foundViolations)
                )));
    }

    private List<String> findViolations(T content, Set<String> forbiddenWords) {
        String value = getter.apply(content);
        List<String> foundViolations = new ArrayList<>();
        for(String word : forbiddenWords) {
            if(value != null && value.contains(word)) {
                foundViolations.add(word);
            }
        }
        return foundViolations;
    }

    private Set<String> fetchForbiddenWords(T content) {
        String contentType = content.getClass().getName().toLowerCase();
        List<ForbiddenWords> forbiddenWordsList = service.findByUserIdAndContentType(content.getUserId(), contentType);
        Set<String> forbiddenWords =
                forbiddenWordsList.stream().map(ForbiddenWords::getWords).flatMap(Set::stream).collect(Collectors.toSet());
        return forbiddenWords;
    }

    public String errorMessage(String fieldName, List<String> forbiddenWords) {
        return "The field '" + fieldName + "' contains the forbidden words: " + String.join(", ", forbiddenWords);
    }

    @Override
    public String getFieldName() {
        return fieldName;
    }

    public UUID getUserId() {
        return userId;
    }
}
