package com.dehold.contentmanager.content.customersupport.service;

import com.dehold.contentmanager.content.customersupport.model.SupportRequest;
import com.dehold.contentmanager.content.customersupport.repository.SupportRequestRepository;
import com.dehold.contentmanager.exception.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SupportRequestService {
    private final SupportRequestRepository repository;

    public SupportRequestService(SupportRequestRepository repository) {
        this.repository = repository;
    }

    public List<SupportRequest> findAll() {
        return repository.findAll();
    }

    public SupportRequest findById(UUID id) {
        return repository.getById(id)
                .orElseThrow(() -> EntityNotFoundException.of("CustomerRequest", id.toString()));
    }

    // Keep the optional version for internal use if needed
    public Optional<SupportRequest> findByIdOptional(UUID id) {
        return repository.getById(id);
    }

    public void createCustomerRequest(SupportRequest supportRequest) {
        repository.create(supportRequest);
    }

    public void updateCustomerRequest(SupportRequest supportRequest) {
        repository.update(supportRequest);
    }

    public void save(SupportRequest supportRequest) {
        repository.create(supportRequest);
    }

    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
}
