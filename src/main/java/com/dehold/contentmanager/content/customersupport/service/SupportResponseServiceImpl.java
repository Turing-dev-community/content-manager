package com.dehold.contentmanager.content.customersupport.service;

import com.dehold.contentmanager.content.customersupport.model.SupportResponse;
import com.dehold.contentmanager.content.customersupport.repository.SupportResponseRepository;
import com.dehold.contentmanager.exception.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class SupportResponseServiceImpl implements SupportResponseService {
    private final SupportResponseRepository repository;

    public SupportResponseServiceImpl(SupportResponseRepository repository) {
        this.repository = repository;
    }

    @Override
    public void createSupportResponse(SupportResponse response) {
        repository.create(response);
    }

    @Override
    public SupportResponse getSupportResponse(UUID id) {
        return repository.getById(id)
                .orElseThrow(() -> EntityNotFoundException.of("SupportResponse", id.toString()));
    }

    @Override
    public void updateSupportResponse(SupportResponse response) {
        repository.update(response);
    }

    @Override
    public void deleteSupportResponse(UUID id) {
        repository.delete(id);
    }

    @Override
    public List<SupportResponse> getSupportResponsesByUserId(UUID userId) {
        return repository.getSupportResponsesByUserId(userId);
    }
}
