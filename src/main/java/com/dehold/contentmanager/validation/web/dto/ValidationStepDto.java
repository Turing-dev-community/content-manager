package com.dehold.contentmanager.validation.web.dto;

import com.dehold.contentmanager.validation.model.ValidationStepType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;
import java.util.UUID;

public class ValidationStepDto {
    private UUID id;
    @NotNull(message = "stepType is required")
    private ValidationStepType stepType;
    @NotBlank(message = "fieldName is required")
    private String fieldName;
    private Map<String, String> parameters;
    private boolean isEnabled = true;

    public ValidationStepDto() {
    }

    public ValidationStepDto(UUID id, ValidationStepType stepType, String fieldName, Map<String, String> parameters,
                             boolean isEnabled) {
        this.id = id;
        this.stepType = stepType;
        this.fieldName = fieldName;
        this.parameters = parameters;
        this.isEnabled = isEnabled;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public ValidationStepType getStepType() {
        return stepType;
    }

    public void setStepType(ValidationStepType stepType) {
        this.stepType = stepType;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public String getPattern() {
        return parameters != null ? parameters.get("pattern") : null;
    }
}
