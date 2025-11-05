package com.dehold.contentmanager.validation.service;

import com.dehold.contentmanager.service.IService;
import com.dehold.contentmanager.validation.model.ValidationPipelineModel;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ValidationPipelineService implements IService<ValidationPipelineModel> {
    @Override
    public ValidationPipelineModel create(ValidationPipelineModel entity) {
        return null;
    }

    @Override
    public ValidationPipelineModel findById(UUID id) {
        return null;
    }

    @Override
    public List<ValidationPipelineModel> findAll() {
        return List.of();
    }

    @Override
    public ValidationPipelineModel update(UUID id, ValidationPipelineModel entity) {
        return null;
    }

    @Override
    public void delete(UUID id) {

    }
}
