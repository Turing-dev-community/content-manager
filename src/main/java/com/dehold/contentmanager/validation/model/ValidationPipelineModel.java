package com.dehold.contentmanager.validation.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class ValidationPipelineModel {
    private UUID id;
    private UUID userId;
    private String contentType;
    private boolean isActive;
    private List<ValidationStepModel> steps;
    private Instant createdAt;

}

