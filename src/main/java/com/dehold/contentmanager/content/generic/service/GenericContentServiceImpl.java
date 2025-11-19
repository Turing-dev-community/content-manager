package com.dehold.contentmanager.content.generic.service;

import com.dehold.contentmanager.content.generic.model.GenericContentModel;
import com.dehold.contentmanager.content.generic.repository.GenericModelRepository;
import com.dehold.contentmanager.exception.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class GenericContentServiceImpl implements GenericContentService {

    private final GenericModelRepository repository;

    public GenericContentServiceImpl(GenericModelRepository repository) {
        this.repository = repository;
    }

    @Override
    public GenericContentModel getById(UUID id) {
        return repository.findById(id).orElseThrow(() -> EntityNotFoundException.of("GenericContent", id.toString()));
    }

    @Override
    public GenericContentModel create(GenericContentModel content) {
        repository.save(content);
        return content;
    }

    @Override
    public GenericContentModel update(GenericContentModel content) {
        getById(content.getId()); // Ensure it exists
        repository.save(content);
        return content;
    }

    @Override
    public void deleteById(UUID id) {
        if (!repository.existsById(id)) {
            throw EntityNotFoundException.of("GenericContent", id.toString());
        }
        repository.deleteById(id);
    }

    @Override
    public boolean existsById(UUID id) {
        return repository.existsById(id);
    }
}
