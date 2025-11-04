package com.dehold.contentmanager.validation.repository;

import com.dehold.contentmanager.validation.model.ValidationPipelineModel;
import com.dehold.contentmanager.validation.model.ValidationStepModel;
import com.dehold.contentmanager.validation.model.ValidationStepType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ValidationPipelineRepositoryTest {

    @Autowired
    ValidationPipelineRepository cut;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM validation_step");
        jdbcTemplate.update("DELETE FROM validation_pipeline");
    }

    @Test
    void givenValidationPipelineDoesNotExist_whenSave_thenInsertsNewPipeline() {
        UUID pipelineId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID stepId = UUID.randomUUID();
        String contentType = "blogpost";
        String description = "Test pipeline";

        Map<String, String> parameters = new HashMap<>();
        parameters.put("minLength", "10");
        parameters.put("maxLength", "100");

        ValidationStepModel step = new ValidationStepModel(
                stepId,
                pipelineId,
                ValidationStepType.LENGTH_VALIDATION,
                "content",
                parameters,
                true
        );

        ValidationPipelineModel pipeline = new ValidationPipelineModel(
                pipelineId,
                userId,
                description,
                contentType,
                List.of(step),
                Instant.now()
        );

        cut.save(pipeline);

        Optional<ValidationPipelineModel> result = cut.findByUserIdAndContentType(userId, contentType);
        assertTrue(result.isPresent());

        ValidationPipelineModel savedPipeline = result.get();
        assertEquals(pipelineId, savedPipeline.getId());
        assertEquals(userId, savedPipeline.getUserId());
        assertEquals(description, savedPipeline.getDescription());
        assertEquals(contentType, savedPipeline.getContentType());
        assertNotNull(savedPipeline.getCreatedAt());

        assertEquals(1, savedPipeline.getSteps().size());
        ValidationStepModel savedStep = savedPipeline.getSteps().getFirst();
        assertEquals(ValidationStepType.LENGTH_VALIDATION, savedStep.getStepType());
        assertEquals("content", savedStep.getFieldName());
        assertEquals(parameters, savedStep.getParameters());
        assertTrue(savedStep.isEnabled());
    }

    @Test
    void givenPipelineExists_whenSave_thenUpdatesExistingPipeline() {
        UUID pipelineId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String contentType = "blogpost";

        ValidationPipelineModel initialPipeline = new ValidationPipelineModel(
                pipelineId,
                userId,
                "Initial description",
                contentType,
                new ArrayList<>(),
                Instant.now()
        );
        cut.save(initialPipeline);

        Map<String, String> parameters = new HashMap<>();
        parameters.put("minLength", "5");

        ValidationStepModel newStep = new ValidationStepModel(
                UUID.randomUUID(),
                pipelineId,
                ValidationStepType.LENGTH_VALIDATION,
                "title",
                parameters,
                true
        );

        ValidationPipelineModel updatedPipeline = new ValidationPipelineModel(
                pipelineId,
                userId,
                "Updated description",
                contentType,
                List.of(newStep),
                null // createdAt should be preserved
        );
        cut.save(updatedPipeline);

        Optional<ValidationPipelineModel> result = cut.findByUserIdAndContentType(userId, contentType);
        assertTrue(result.isPresent());

        ValidationPipelineModel savedPipeline = result.get();
        assertEquals(pipelineId, savedPipeline.getId());
        assertEquals("Updated description", savedPipeline.getDescription());
        assertEquals(1, savedPipeline.getSteps().size());
        assertEquals("title", savedPipeline.getSteps().getFirst().getFieldName());
    }

    @Test
    void givenPipelineWithMultipleSteps_whenSave_thenSavesAllSteps() {
        UUID pipelineId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Map<String, String> lengthParams = new HashMap<>();
        lengthParams.put("minLength", "10");
        lengthParams.put("maxLength", "100");

        Map<String, String> forbiddenParams = new HashMap<>();
        forbiddenParams.put("words", "spam,badword");

        ValidationStepModel lengthStep = new ValidationStepModel(
                UUID.randomUUID(),
                pipelineId,
                ValidationStepType.LENGTH_VALIDATION,
                "content",
                lengthParams,
                true
        );

        ValidationStepModel forbiddenStep = new ValidationStepModel(
                UUID.randomUUID(),
                pipelineId,
                ValidationStepType.FORBIDDEN_WORD_VALIDATION,
                "title",
                forbiddenParams,
                false
        );

        ValidationPipelineModel pipeline = new ValidationPipelineModel(
                pipelineId,
                userId,
                "Multi-step pipeline",
                "blogpost",
                List.of(lengthStep, forbiddenStep),
                Instant.now()
        );

        cut.save(pipeline);

        Optional<ValidationPipelineModel> result = cut.findByUserIdAndContentType(userId, "blogpost");
        assertTrue(result.isPresent());

        ValidationPipelineModel savedPipeline = result.get();
        assertEquals(2, savedPipeline.getSteps().size());

        ValidationStepModel savedLengthStep = savedPipeline.getSteps().stream()
                .filter(s -> s.getStepType() == ValidationStepType.LENGTH_VALIDATION)
                .findFirst()
                .orElseThrow();
        assertEquals("content", savedLengthStep.getFieldName());
        assertEquals(lengthParams, savedLengthStep.getParameters());
        assertTrue(savedLengthStep.isEnabled());

        ValidationStepModel savedForbiddenStep = savedPipeline.getSteps().stream()
                .filter(s -> s.getStepType() == ValidationStepType.FORBIDDEN_WORD_VALIDATION)
                .findFirst()
                .orElseThrow();
        assertEquals("title", savedForbiddenStep.getFieldName());
        assertEquals(forbiddenParams, savedForbiddenStep.getParameters());
        assertFalse(savedForbiddenStep.isEnabled());
    }

    @Test
    void givenNoPipelines_whenFindAll_thenReturnsEmptyList() {
        List<ValidationPipelineModel> result = cut.findAll();

        assertTrue(result.isEmpty());
    }

    @Test
    void givenMultiplePipelines_whenFindAll_thenReturnsAllPipelines() {
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();

        ValidationPipelineModel pipeline1 = new ValidationPipelineModel(
                UUID.randomUUID(),
                userId1,
                "Pipeline 1",
                "blogpost",
                new ArrayList<>(),
                Instant.now().minusSeconds(100)
        );

        ValidationPipelineModel pipeline2 = new ValidationPipelineModel(
                UUID.randomUUID(),
                userId2,
                "Pipeline 2",
                "supportrequest",
                new ArrayList<>(),
                Instant.now()
        );

        cut.save(pipeline1);
        cut.save(pipeline2);

        List<ValidationPipelineModel> result = cut.findAll();

        assertEquals(2, result.size());
        assertEquals("Pipeline 1", result.get(0).getDescription()); // Should be ordered by created_at
        assertEquals("Pipeline 2", result.get(1).getDescription());
    }

    @Test
    void givenPipelineExists_whenFindByUserIdAndContentType_thenReturnsPipelineAndSteps() {
        UUID userId = UUID.randomUUID();
        String contentType = "blogpost";
        UUID pipelineId = UUID.randomUUID();

        Map<String, String> lengthParams = new HashMap<>();
        lengthParams.put("minLength", "5");
        lengthParams.put("maxLength", "200");

        Map<String, String> forbiddenParams = new HashMap<>();
        forbiddenParams.put("words", "test,example");

        ValidationStepModel lengthStep = new ValidationStepModel(
                UUID.randomUUID(),
                pipelineId,
                ValidationStepType.LENGTH_VALIDATION,
                "title",
                lengthParams,
                true
        );

        ValidationStepModel forbiddenStep = new ValidationStepModel(
                UUID.randomUUID(),
                pipelineId,
                ValidationStepType.FORBIDDEN_WORD_VALIDATION,
                "content",
                forbiddenParams,
                true
        );

        ValidationPipelineModel pipeline = new ValidationPipelineModel(
                pipelineId,
                userId,
                "Test pipeline",
                contentType,
                List.of(lengthStep, forbiddenStep),
                Instant.now()
        );
        cut.save(pipeline);

        Optional<ValidationPipelineModel> result = cut.findByUserIdAndContentType(userId, contentType);

        assertTrue(result.isPresent());
        ValidationPipelineModel foundPipeline = result.get();
        assertEquals("Test pipeline", foundPipeline.getDescription());
        assertEquals(2, foundPipeline.getSteps().size());

        ValidationStepModel foundLengthStep = foundPipeline.getSteps().stream()
                .filter(s -> s.getStepType() == ValidationStepType.LENGTH_VALIDATION)
                .findFirst()
                .orElseThrow();
        assertEquals("title", foundLengthStep.getFieldName());
        assertEquals(lengthParams, foundLengthStep.getParameters());
        assertTrue(foundLengthStep.isEnabled());

        ValidationStepModel foundForbiddenStep = foundPipeline.getSteps().stream()
                .filter(s -> s.getStepType() == ValidationStepType.FORBIDDEN_WORD_VALIDATION)
                .findFirst()
                .orElseThrow();
        assertEquals("content", foundForbiddenStep.getFieldName());
        assertEquals(forbiddenParams, foundForbiddenStep.getParameters());
        assertTrue(foundForbiddenStep.isEnabled());
    }

    @Test
    void givenPipelineDoesNotExist_whenFindByUserIdAndContentType_thenReturnsEmpty() {
        Optional<ValidationPipelineModel> result = cut.findByUserIdAndContentType(UUID.randomUUID(), "nonexistent");

        assertFalse(result.isPresent());
    }

    @Test
    void givenPipelineExists_whenDeleteById_thenRemovesPipelineAndSteps() {
        UUID pipelineId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Map<String, String> parameters = new HashMap<>();
        parameters.put("minLength", "10");

        ValidationStepModel step = new ValidationStepModel(
                UUID.randomUUID(),
                pipelineId,
                ValidationStepType.LENGTH_VALIDATION,
                "content",
                parameters,
                true
        );

        ValidationPipelineModel pipeline = new ValidationPipelineModel(
                pipelineId,
                userId,
                "Test pipeline",
                "blogpost",
                List.of(step),
                Instant.now()
        );
        cut.save(pipeline);

        assertTrue(cut.findByUserIdAndContentType(userId, "blogpost").isPresent());

        cut.deleteById(pipelineId);

        assertFalse(cut.findByUserIdAndContentType(userId, "blogpost").isPresent());

        int stepCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM validation_step WHERE pipeline_id = ?",
                Integer.class,
                pipelineId
        );
        assertEquals(0, stepCount);
    }
}