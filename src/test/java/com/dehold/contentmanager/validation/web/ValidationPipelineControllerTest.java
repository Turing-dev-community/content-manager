package com.dehold.contentmanager.validation.web;

import com.dehold.contentmanager.validation.model.ValidationPipelineModel;
import com.dehold.contentmanager.validation.model.ValidationStepType;
import com.dehold.contentmanager.validation.web.dto.ValidationPipelineCreateDto;
import com.dehold.contentmanager.validation.web.dto.ValidationPipelineUpdateDto;
import com.dehold.contentmanager.validation.web.dto.ValidationStepDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ValidationPipelineControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void givenCreateValidationPipelineRequest_whenCreateValidationPipeline_thenReturnsCreatedPipeline() {
        var requestDto = new ValidationPipelineCreateDto();
        requestDto.setUserId(UUID.randomUUID());
        requestDto.setContentType("BlogPost");
        requestDto.setSteps(List.of(
                new ValidationStepDto(null, ValidationStepType.LENGTH_VALIDATION, "title", Map.of("minLength", "10",
                        "maxLength", "500"), true)
        ));

        var response = restTemplate.postForEntity("http://localhost:" + port + "/api/validation-pipeline",
                requestDto, ValidationPipelineModel.class);

        assertEquals(201, response.getStatusCode().value());
        var createdPipeline = response.getBody();
        assertNotNull(createdPipeline);
        assertEquals(requestDto.getUserId(), createdPipeline.getUserId());
        assertEquals(requestDto.getContentType(), createdPipeline.getContentType());
        assertEquals(1, createdPipeline.getSteps().size());
        assertEquals(ValidationStepType.LENGTH_VALIDATION, createdPipeline.getSteps().getFirst().getStepType());
    }

    @Test
    void givenUpdateRequest_whenValidationPipelineExists_thenUpdatesPipeline() {
        var createRequestDto = new ValidationPipelineCreateDto();
        createRequestDto.setUserId(UUID.randomUUID());
        createRequestDto.setContentType("BlogPost");
        createRequestDto.setSteps(List.of(
                new ValidationStepDto(null, ValidationStepType.LENGTH_VALIDATION, "title", Map.of("minLength", "10",
                        "maxLength", "500"), true)
        ));

        var createResponse = restTemplate.postForEntity("http://localhost:" + port + "/api/validation-pipeline",
                createRequestDto, ValidationPipelineModel.class);
        var createdPipeline = createResponse.getBody();
        assertEquals(201, createResponse.getStatusCode().value());
        assertNotNull(createdPipeline);

        var updateRequestDto = new ValidationPipelineUpdateDto();
        updateRequestDto.setId(createdPipeline.getId());
        updateRequestDto.setUserId(createdPipeline.getUserId());
        updateRequestDto.setContentType(createdPipeline.getContentType());
        UUID validationStepId = createdPipeline.getSteps().getFirst().getId();
        updateRequestDto.setSteps(List.of(
                new ValidationStepDto(validationStepId, ValidationStepType.LENGTH_VALIDATION, "content", Map.of("minLength", "50",
                        "maxLength", "2000"), true)
        ));

        HttpEntity<ValidationPipelineUpdateDto> requestEntity = new HttpEntity<>(updateRequestDto);
        ResponseEntity<ValidationPipelineModel> response =
                restTemplate.exchange("http://localhost:" + port + "/api/validation-pipeline/" +
                        createdPipeline.getId(), HttpMethod.PUT,
                        requestEntity, ValidationPipelineModel.class);

        assertEquals(200, response.getStatusCode().value());
        var updatedPipeline = response.getBody();
        assertNotNull(updatedPipeline);
        assertEquals(updatedPipeline.getId(), createdPipeline.getId());
        assertEquals(1, updatedPipeline.getSteps().size());
        assertEquals(validationStepId, updatedPipeline.getSteps().getFirst().getId());
        assertEquals("content", updatedPipeline.getSteps().getFirst().getFieldName());
    }

}