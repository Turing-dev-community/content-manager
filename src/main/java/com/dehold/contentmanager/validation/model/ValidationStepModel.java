package com.dehold.contentmanager.validation.model;

import java.util.Map;
import java.util.UUID;

public class ValidationStepModel {
    private UUID id;
    private UUID pipelineId;
    private ValidationStepType stepType;
    private String fieldName;
    private Map<String, String> parameters;
    private boolean isEnabled;


    public ValidationStepModel(UUID id, UUID pipelineId, ValidationStepType stepType, String fieldName, Map<String, String> parameters, boolean isEnabled) {
        this.id = id;
        this.pipelineId = pipelineId;
        this.stepType = stepType;
        this.fieldName = fieldName;
        this.parameters = parameters;
        this.isEnabled = isEnabled;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public ValidationStepType getStepType() {
        return stepType;
    }

    public void setStepType(ValidationStepType stepType) {
        this.stepType = stepType;
    }

    public UUID getPipelineId() {
        return pipelineId;
    }

    public void setPipelineId(UUID pipelineId) {
        this.pipelineId = pipelineId;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }
}
