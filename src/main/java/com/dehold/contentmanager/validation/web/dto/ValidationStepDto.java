package com.dehold.contentmanager.validation.web.dto;

import com.dehold.contentmanager.validation.model.ValidationStepType;

import java.util.Map;
import java.util.UUID;

public class ValidationStepDto {
    private ValidationStepType stepType;
    private String fieldName;
    private Map<String, String> parameters;
    private boolean isEnabled;

    public ValidationStepDto(ValidationStepType stepType, String fieldName, Map<String, String> parameters,
                             boolean isEnabled) {
        this.stepType = stepType;
        this.fieldName = fieldName;
        this.parameters = parameters;
        this.isEnabled = isEnabled;
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
}
