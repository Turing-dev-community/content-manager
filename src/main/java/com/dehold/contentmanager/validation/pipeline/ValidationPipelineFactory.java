package com.dehold.contentmanager.validation.pipeline;

import com.dehold.contentmanager.content.Content;
import com.dehold.contentmanager.content.blogpost.model.BlogPost;
import com.dehold.contentmanager.validation.model.ContentTypeRegistry;
import com.dehold.contentmanager.validation.model.ValidationPipelineModel;
import com.dehold.contentmanager.validation.model.ValidationStepModel;
import com.dehold.contentmanager.validation.service.ValidationPipelineService;
import com.dehold.contentmanager.validation.step.ValidationStep;
import com.dehold.contentmanager.validation.step.ValidationStepFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import static org.springframework.util.StringUtils.capitalize;

@Component
public class ValidationPipelineFactory {

    @Autowired
    ValidationPipelineService service;

    @Autowired
    ValidationStepFactory stepFactory;


    public ValidationPipelineFactory(ValidationPipelineService service, ValidationStepFactory stepFactory) {
        this.service = service;
        this.stepFactory = stepFactory;
    }

    public <T extends Content> List<ValidationPipeline<T>> createValidationPipelineForUserAndContentType(UUID userId,
                                                                                                       String contentType) {
        List<ValidationPipelineModel> pipelineModels = service.findByUserIdAndContentType(userId, contentType);

        List<ValidationPipeline<T>> pipelines = new ArrayList<>();
        for (ValidationPipelineModel pipelineModel : pipelineModels) {
            ValidationPipelineBuilder<T> builder = new ValidationPipelineBuilder<>();
            List<ValidationStep<T>> steps = new ArrayList<>();
            for(ValidationStepModel stepModel : pipelineModel.getSteps()) {
                ValidationStep<T> step = stepFactory.createValidationStepFromModel(stepModel,
                    getFieldExtractor(stepModel.getFieldName(), contentType), userId);
                steps.add(step);
            }
            steps.forEach(builder::addStep);
            pipelines.add(builder.build());
        }

        return pipelines;
    }

    private <T extends Content> Function<T, String> getFieldExtractor(String fieldName, String contentType) {
        Class<? extends Content> contentClass = ContentTypeRegistry.CONTENT_TYPES.get(contentType.toLowerCase());
        if (contentClass == null) {
            throw new IllegalArgumentException("Unknown content type: " + contentType);
        }

        String getterMethodName = "get" + capitalize(fieldName);

        try {
            Method getterMethod = contentClass.getMethod(getterMethodName);
            return (Function<T, String>) (content -> {
                try {
                    Object result = getterMethod.invoke(content);
                    return result != null ? result.toString() : null;
                } catch (Exception e) {
                    throw new RuntimeException("Failed to extract field: " + fieldName, e);
                }
            });
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Field '" + fieldName + "' not found in class: " + contentClass.getSimpleName());
        }
    }

}
