package com.dehold.contentmanager.validation.step;

import com.dehold.contentmanager.content.Content;
import com.dehold.contentmanager.validation.model.ValidationResult;

import java.util.function.Function;

public class ForbiddenWordValidator<T extends Content> implements ValidationStep<T> {

    private final Function<T, String> getter;
    private final String fieldName;

    public ForbiddenWordValidator(Function<T, String> getter, String fieldName) {
        this.getter = getter;
        this.fieldName = fieldName;
    }


    @Override
    public ValidationResult validate(T content) {
        return null;
    }

    @Override
    public String getFieldName() {
        return "";
    }
}
