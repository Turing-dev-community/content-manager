package com.dehold.contentmanager.validation.web;


import com.dehold.contentmanager.validation.model.ValidationPipelineModel;
import com.dehold.contentmanager.validation.service.ValidationPipelineService;
import com.dehold.contentmanager.validation.web.dto.ValidationPipelineCreateDto;
import com.dehold.contentmanager.validation.web.dto.ValidationPipelineUpdateDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/validation-pipelines")
public class ValidationPipelineController {

    @Autowired
    private final ValidationPipelineService validationPipelineService;

    public ValidationPipelineController(ValidationPipelineService validationPipelineService) {
        this.validationPipelineService = validationPipelineService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ValidationPipelineModel> getValidationPipeline(@PathVariable UUID id) {
        ValidationPipelineModel model = validationPipelineService.findById(id);
        return new ResponseEntity<>(model, HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<List<ValidationPipelineModel>> getValidationPipelineByUserIdAndContentType(
            @RequestParam UUID userId,
            @RequestParam String contentType) {
        List<ValidationPipelineModel> response = validationPipelineService.findByUserIdAndContentType(userId,
                contentType);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }


    @PostMapping
    public ResponseEntity<ValidationPipelineModel> createValidationPipeline(@RequestBody ValidationPipelineCreateDto dto) {
        ValidationPipelineModel model = validationPipelineService.create(dto.toModel());
        return new ResponseEntity<>(model, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ValidationPipelineModel> updateValidationPipeline(@PathVariable UUID id,
                                                                            @RequestBody ValidationPipelineUpdateDto dto) {
        ValidationPipelineModel model = validationPipelineService.update(dto.toModel());
        return new ResponseEntity<>(model, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteValidationPipeline(@PathVariable UUID id) {
        validationPipelineService.delete(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
