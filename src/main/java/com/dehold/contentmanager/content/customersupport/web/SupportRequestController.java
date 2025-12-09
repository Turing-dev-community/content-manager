package com.dehold.contentmanager.content.customersupport.web;

import com.dehold.contentmanager.content.customersupport.model.SupportRequest;
import com.dehold.contentmanager.content.customersupport.service.SupportRequestService;
import com.dehold.contentmanager.content.customersupport.web.dto.CustomerRequestDto;
import com.dehold.contentmanager.content.customersupport.web.dto.PartialResultResponse;
import com.dehold.contentmanager.content.customersupport.web.dto.SubscribeRequestDto;
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

    /**
     * Get all customer requests with retry logic
     * Returns PartialResultResponse which includes successful results and any failed IDs
     */
    @GetMapping
    public ResponseEntity<PartialResultResponse<CustomerRequestDto>> getAll() {
        PartialResultResponse<SupportRequest> result = service.findAllWithRetry();
        
        PartialResultResponse<CustomerRequestDto> response = new PartialResultResponse<>(
            result.getSuccessfulResults().stream()
                    .map(this::toDto)
                    .collect(Collectors.toList()),
            result.getFailedIds(),
            result.getRetryInfo()
        );
        response.setPartial(result.isPartial());
        response.setTimestamp(result.getTimestamp());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get customer request by ID with retry logic
     * Returns PartialResultResponse which includes the successful result or failed ID info
     */
    @GetMapping("/{id}")
    public ResponseEntity<PartialResultResponse<CustomerRequestDto>> getById(@PathVariable UUID id) {
        PartialResultResponse<SupportRequest> result = service.findByIdWithRetry(id);
        
        PartialResultResponse<CustomerRequestDto> response = new PartialResultResponse<>(
            result.getSuccessfulResults().stream()
                    .map(this::toDto)
                    .collect(Collectors.toList()),
            result.getFailedIds(),
            result.getRetryInfo()
        );
        response.setPartial(result.isPartial());
        response.setTimestamp(result.getTimestamp());
        
        // Return 206 (Partial Content) if response is partial, 200 if successful
        return ResponseEntity.status(response.isPartial() ? HttpStatus.PARTIAL_CONTENT : HttpStatus.OK)
                .body(response);
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

    @PostMapping("/{id}/subscribe")
    public ResponseEntity<Void> subscribe(@PathVariable UUID id, @RequestBody SubscribeRequestDto body) {
        service.addSubscriber(id, body.getUserId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/unsubscribe")
    public ResponseEntity<Void> unsubscribe(@PathVariable UUID id, @RequestBody SubscribeRequestDto body) {
        service.removeSubscriber(id, body.getUserId());
        return ResponseEntity.ok().build();
    }

    private CustomerRequestDto toDto(SupportRequest entity) {
        CustomerRequestDto dto = new CustomerRequestDto();
        dto.setId(entity.getId());
        dto.setText(entity.getText());
        dto.setSupportResponse(entity.getSupportResponse());
        dto.setCustomerId(entity.getCustomerId());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setSubscribers(entity.getSubscribers());
        return dto;
    }
}
