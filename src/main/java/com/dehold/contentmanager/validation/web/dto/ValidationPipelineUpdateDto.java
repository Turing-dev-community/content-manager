package com.dehold.contentmanager.validation.web.dto;

import com.dehold.contentmanager.validation.model.ValidationPipelineModel;
import com.dehold.contentmanager.validation.model.ValidationStepModel;
import java.util.List;
import java.util.UUID;

public class ValidationPipelineUpdateDto {
    private UUID id;
    private UUID userId;
    private String description;
    private String contentType;
    private List<ValidationStepDto> steps;

    public static ValidationPipelineUpdateDto fromModel(ValidationPipelineModel model) {
        ValidationPipelineUpdateDto dto = new ValidationPipelineUpdateDto();
        dto.setId(model.getId());
        dto.setUserId(model.getUserId());
        dto.setDescription(model.getDescription());
        dto.setContentType(model.getContentType());
        if (model.getSteps() != null) {
            dto.setSteps(model.getSteps().stream()
                    .map(s -> new ValidationStepDto(
                            s.getId(),
                            s.getStepType(),
                            s.getFieldName(),
                            s.getParameters(),
                            s.isEnabled()
                    ))
                    .toList());
        }
        return dto;
    }

    public ValidationPipelineModel toModel() {
        return new ValidationPipelineModel(
                this.getId(),
                this.getUserId(),
                this.getDescription(),
                this.getContentType(),
                this.getSteps() != null ? this.getSteps().stream()
                        .map(s -> new ValidationStepModel(
                                s.getId(),
                                this.getId(),
                                s.getStepType(),
                                s.getFieldName(),
                                s.getParameters(),
                                s.isEnabled()
                        ))
                        .toList() : null,
                null
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public List<ValidationStepDto> getSteps() {
        return steps;
    }

    public void setSteps(List<ValidationStepDto> steps) {
        this.steps = steps;
    }

}
