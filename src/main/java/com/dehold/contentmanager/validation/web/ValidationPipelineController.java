package com.dehold.contentmanager.validation.web;


import com.dehold.contentmanager.validation.model.ValidationPipelineModel;
import com.dehold.contentmanager.validation.service.ValidationPipelineService;
import com.dehold.contentmanager.validation.web.dto.ValidationPipelineCreateDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/validation-pipeline")
public class ValidationPipelineController {

    @Autowired
    private final ValidationPipelineService validationPipelineService;

    public ValidationPipelineController(ValidationPipelineService validationPipelineService) {
        this.validationPipelineService = validationPipelineService;
    }


    @PostMapping
    public ResponseEntity<ValidationPipelineModel> createValidationPipeline(@RequestBody ValidationPipelineCreateDto dto) {
        ValidationPipelineModel model = validationPipelineService.create(dto.toModel());
        return new ResponseEntity<>(model, HttpStatus.CREATED);
    }

}
