package com.dehold.contentmanager.validation.model;

import java.util.Map;
import java.util.UUID;

public class ValidationStepModel {
    private UUID id;
    private UUID pipelineId;
    private ValidationStepType stepType;
    private String fieldName;
    private int stepOrder;
    private Map<String, String> parameters;
    private boolean isEnabled;
}
