package com.dehold.contentmanager.content.customersupport.web;

import com.dehold.contentmanager.content.customersupport.model.SupportRequest;
import com.dehold.contentmanager.content.customersupport.service.SupportRequestService;
import com.dehold.contentmanager.content.customersupport.web.dto.CustomerRequestDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/customer-requests")
public class SupportRequestController {
    private final SupportRequestService service;

    public SupportRequestController(SupportRequestService service) {
        this.service = service;
    }

    @GetMapping
    public List<CustomerRequestDto> getAll() {
        return service.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerRequestDto> getById(@PathVariable UUID id) {
        SupportRequest request = service.findById(id); // This will throw EntityNotFoundException if not found
        return ResponseEntity.ok(toDto(request));
    }

    @PostMapping
    public ResponseEntity<CustomerRequestDto> create(@RequestBody CustomerRequestDto dto) {
        SupportRequest entity = new SupportRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                dto.getText(),
                dto.getSupportResponse(),
                dto.getCustomerId(),
                Instant.now(),
                Instant.now()
        );
        service.createCustomerRequest(entity);
        return new ResponseEntity<>(toDto(entity), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomerRequestDto> update(@PathVariable UUID id, @RequestBody CustomerRequestDto dto) {
        SupportRequest existing = service.findById(id); // This will throw EntityNotFoundException if not found
        SupportRequest entity = new SupportRequest(
                id,
                UUID.randomUUID(),
                dto.getText(),
                dto.getSupportResponse(),
                dto.getCustomerId(),
                existing.getCreatedAt(),
                Instant.now()
        );
        service.updateCustomerRequest(entity);
        return ResponseEntity.ok(toDto(entity));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private CustomerRequestDto toDto(SupportRequest entity) {
        CustomerRequestDto dto = new CustomerRequestDto();
        dto.setId(entity.getId());
        dto.setText(entity.getText());
        dto.setSupportResponse(entity.getSupportResponse());
        dto.setCustomerId(entity.getCustomerId());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
