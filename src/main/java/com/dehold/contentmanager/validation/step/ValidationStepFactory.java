package com.dehold.contentmanager.validation.step;

import com.dehold.contentmanager.content.Content;
import com.dehold.contentmanager.validation.service.ForbiddenWordsService;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.function.Function;

@Component
public class ValidationStepFactory {

    private final ForbiddenWordsService forbiddenWordsService;

    public ValidationStepFactory(ForbiddenWordsService forbiddenWordsService) {
        this.forbiddenWordsService = forbiddenWordsService;
    }

    public <T extends Content> ForbiddenWordValidator<T> createForbiddenWordValidator(
            Function<T, String> getter, String fieldName, UUID userId) {
        return new ForbiddenWordValidator<>(forbiddenWordsService, getter, fieldName, userId);
    }
}

