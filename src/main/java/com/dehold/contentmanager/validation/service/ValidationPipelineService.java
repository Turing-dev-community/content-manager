package com.dehold.contentmanager.validation.service;

import com.dehold.contentmanager.exception.EntityNotFoundException;
import com.dehold.contentmanager.service.IService;
import com.dehold.contentmanager.validation.model.ValidationPipelineModel;
import com.dehold.contentmanager.validation.repository.ValidationPipelineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ValidationPipelineService {

    @Autowired
    private final ValidationPipelineRepository repository;

    public ValidationPipelineService(ValidationPipelineRepository repository) {
        this.repository = repository;
    }

    public ValidationPipelineModel create(ValidationPipelineModel entity) {
        repository.save(entity);
        return entity;
    }

    public ValidationPipelineModel findByUserIdAndContentType(UUID userId, String contentType) {
        return repository.findByUserIdAndContentType(userId, contentType).orElse(null);
    }


    public List<ValidationPipelineModel> findAll() {
        return repository.findAll();
    }

    public ValidationPipelineModel update(ValidationPipelineModel entity) {
        repository.save(entity);
        return entity;
    }

    public void delete(UUID id) {
        repository.deleteById(id);
    }

    public ValidationPipelineModel findById(UUID id) {
        return repository.findById(id).orElseThrow(() -> EntityNotFoundException.of("ValidationPipeline",
                id.toString()));
    }
}
