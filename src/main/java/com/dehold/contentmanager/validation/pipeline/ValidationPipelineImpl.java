package com.dehold.contentmanager.validation.pipeline;

import com.dehold.contentmanager.content.Content;
import com.dehold.contentmanager.validation.model.ValidationError;
import com.dehold.contentmanager.validation.model.ValidationResult;
import com.dehold.contentmanager.validation.step.ValidationStep;

import java.util.ArrayList;
import java.util.List;

public final class ValidationPipelineImpl<T extends Content> implements ValidationPipeline<T> {
    private final List<ValidationStep<T>> validationSteps;

    public ValidationPipelineImpl(List<ValidationStep<T>> validationSteps) {
        this.validationSteps = validationSteps;
    }

    @Override
    public ValidationResult run(T content) {
        List<ValidationError> validationErrors = new ArrayList<>();
        for(ValidationStep<T> step : validationSteps) {
            var result = step.validate(content);
            if(!result.isValid()) {
                validationErrors.addAll(result.getErrors());
            }
        }
        return validationErrors.isEmpty() ? ValidationResult.valid(content.getClass().getSimpleName(),
                content.getId(), content.getUserId()) :
                ValidationResult.invalid(content.getClass().getSimpleName(),
                        content.getId(), content.getUserId(), validationErrors);
    }
}
