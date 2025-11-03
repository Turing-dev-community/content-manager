package com.dehold.contentmanager.validation.step;

import com.dehold.contentmanager.content.Content;
import com.dehold.contentmanager.validation.model.ValidationResult;
import com.dehold.contentmanager.validation.service.ForbiddenWordsService;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.function.Function;

@Component
public class ForbiddenWordValidator<T extends Content> implements ValidationStep<T> {

    private final ForbiddenWordsService service;
    private final Function<T, String> getter;
    private final String fieldName;
    private final UUID userId;

    public ForbiddenWordValidator(ForbiddenWordsService service, Function<T, String> getter, String fieldName,
                                  UUID userId) {
        this.service = service;
        this.getter = getter;
        this.fieldName = fieldName;
        this.userId = userId;
    }


    @Override
    public ValidationResult validate(T content) {
        return null;
    }

    @Override
    public String getFieldName() {
        return fieldName;
    }

    public UUID getUserId() {
        return userId;
    }
}
