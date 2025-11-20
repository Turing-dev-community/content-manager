package com.dehold.contentmanager.validation.service;

import com.dehold.contentmanager.content.blogpost.service.BlogPostService;
import com.dehold.contentmanager.content.customersupport.repository.SupportRequestRepository;
import com.dehold.contentmanager.content.customersupport.service.SupportResponseService;
import com.dehold.contentmanager.content.generic.service.GenericContentService;
import com.dehold.contentmanager.validation.model.ValidationError;
import com.dehold.contentmanager.validation.model.ValidationResult;
import com.dehold.contentmanager.validation.pipeline.ValidationPipelineFactory;
import com.dehold.contentmanager.validation.repository.ValidationResultRepository;
import com.dehold.contentmanager.validation.web.dto.ValidationReportDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ValidationServiceGenerateReportTest {
    @Mock
    private ValidationResultRepository repository;

    @InjectMocks
    private ValidationServiceImpl service;

    private ValidationPipelineFactory pipelineFactory;
    private BlogPostService blogPostService;
    private SupportRequestRepository supportRepo;
    private SupportResponseService supportResponseService;
    private GenericContentService genericContentService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        pipelineFactory = mock(ValidationPipelineFactory.class);
        blogPostService = mock(BlogPostService.class);
        supportRepo = mock(SupportRequestRepository.class);
        supportResponseService = mock(SupportResponseService.class);
        genericContentService = mock(GenericContentService.class);
        repository = Mockito.mock(ValidationResultRepository.class);
        service = new ValidationServiceImpl(
                repository,
                pipelineFactory,
                blogPostService,
                supportResponseService,
                supportRepo,
                genericContentService
        );
    }

    @Test
    void summaryMode_countsAllErrorsAndCodesCorrectly() {
        UUID userId = UUID.randomUUID();

        ValidationError e1 = Mockito.mock(ValidationError.class);
        when(e1.code()).thenReturn("LENGTH_VALIDATION_FAILED");

        ValidationError e2 = Mockito.mock(ValidationError.class);
        when(e2.code()).thenReturn("FORBIDDEN_WORD_VALIDATION_FAILED");

        ValidationResult vr1 = Mockito.mock(ValidationResult.class);
        when(vr1.getErrors()).thenReturn(List.of(e1));

        ValidationResult vr2 = Mockito.mock(ValidationResult.class);
        when(vr2.getErrors()).thenReturn(List.of(e1, e2));

        when(repository.findByUserId(userId)).thenReturn(List.of(vr1, vr2));

        ValidationReportDto dto = service.generateValidationReport(userId, false);

        assertNotNull(dto);
        assertEquals(3, dto.getTotalErrorCount(), "Total error count should be sum of all errors");
        Map<String, String> codeMap = dto.getErrorCodeToErrorCount();
        assertNotNull(codeMap);
        assertEquals("2", codeMap.get("LENGTH_VALIDATION_FAILED"));
        assertEquals("1", codeMap.get("FORBIDDEN_WORD_VALIDATION_FAILED"));

        // In summary mode detailed maps should be null or empty
        assertTrue(dto.getErrorCountsPerContentTypes() == null || dto.getErrorCountsPerContentTypes().isEmpty());
        assertTrue(dto.getErrorCountsPerContentTypesAndErrorCode() == null || dto.getErrorCountsPerContentTypesAndErrorCode().isEmpty());
    }

    @Test
    void detailedMode_producesPerContentAndPerContentPerCodeCounts() {
        UUID userId = UUID.randomUUID();

        ValidationError eLen = Mockito.mock(ValidationError.class);
        when(eLen.code()).thenReturn("LENGTH_VALIDATION_FAILED");

        ValidationError ePhone = Mockito.mock(ValidationError.class);
        when(ePhone.code()).thenReturn("PHONE_NUMBER_FORBIDDEN_VALIDATION_FAILED");

        // vr1: two errors
        ValidationResult vr1 = Mockito.mock(ValidationResult.class);
        when(vr1.getErrors()).thenReturn(List.of(eLen, ePhone));
        // vr1.getContentType() can be null in tests; service will use "unknown" key.
        when(vr1.getContentType()).thenReturn(null);

        // vr2: one LENGTH error
        ValidationResult vr2 = Mockito.mock(ValidationResult.class);
        when(vr2.getErrors()).thenReturn(List.of(eLen));
        when(vr2.getContentType()).thenReturn(null);

        when(repository.findByUserId(userId)).thenReturn(List.of(vr1, vr2));

        ValidationReportDto dto = service.generateValidationReport(userId, true);

        assertNotNull(dto);
        assertEquals(3, dto.getTotalErrorCount(), "Total errors should be 3");

        Map<String, String> global = dto.getErrorCodeToErrorCount();
        assertNotNull(global);
//        assertEquals(2, global.get("LENGTH_VALIDATION_FAILED").intValue());
//        assertEquals(1, global.get("PHONE_NUMBER_FORBIDDEN_VALIDATION_FAILED").intValue());

        // detailed maps should be present
        Map<String, Integer> perContent = dto.getErrorCountsPerContentTypes();
        assertNotNull(perContent);
        // since contentType was mocked as null, service uses "unknown" key
        assertTrue(perContent.containsKey("unknown"));
        assertEquals(3, perContent.get("unknown").intValue());

        Map<String, Map<String, Integer>> perContentAndCode = dto.getErrorCountsPerContentTypesAndErrorCode();
        assertNotNull(perContentAndCode);
        assertTrue(perContentAndCode.containsKey("unknown"));
        Map<String, Integer> inner = perContentAndCode.get("unknown");
        assertEquals(2, inner.get("LENGTH_VALIDATION_FAILED").intValue());
        assertEquals(1, inner.get("PHONE_NUMBER_FORBIDDEN_VALIDATION_FAILED").intValue());
    }

    @Test
    void emptyResults_returnZeroCountsBothModes() {
        UUID userId = UUID.randomUUID();
        when(repository.findByUserId(userId)).thenReturn(List.of());

        ValidationReportDto dtoSummary = service.generateValidationReport(userId, false);
        assertNotNull(dtoSummary);
        assertEquals(0, dtoSummary.getTotalErrorCount());
        assertTrue(dtoSummary.getErrorCodeToErrorCount().isEmpty());

        ValidationReportDto dtoDetailed = service.generateValidationReport(userId, true);
        assertNotNull(dtoDetailed);
        assertEquals(0, dtoDetailed.getTotalErrorCount());
        assertTrue(dtoDetailed.getErrorCodeToErrorCount().isEmpty());
        assertTrue(dtoDetailed.getErrorCountsPerContentTypes().isEmpty());
        assertTrue(dtoDetailed.getErrorCountsPerContentTypesAndErrorCode().isEmpty());
    }

    @Test
    void nullUserId_returnsEmptyReport() {
        ValidationReportDto dto = service.generateValidationReport(null, true);
        assertNotNull(dto);
        assertEquals(0, dto.getTotalErrorCount());
        assertTrue(dto.getErrorCodeToErrorCount().isEmpty());
        assertTrue(dto.getErrorCountsPerContentTypes().isEmpty());
        assertTrue(dto.getErrorCountsPerContentTypesAndErrorCode().isEmpty());
    }

    @Test
    void handlesValidationErrorWithNullCode_andIgnoresEmptyErrorLists() {
        UUID userId = UUID.randomUUID();

        ValidationError eNull = Mockito.mock(ValidationError.class);
        when(eNull.code()).thenReturn(null);

        ValidationResult vrWithNullCode = Mockito.mock(ValidationResult.class);
        when(vrWithNullCode.getErrors()).thenReturn(List.of(eNull));
        when(vrWithNullCode.getContentType()).thenReturn(null);

        // result with empty errors should be ignored
        ValidationResult vrEmpty = Mockito.mock(ValidationResult.class);
        when(vrEmpty.getErrors()).thenReturn(List.of());

        when(repository.findByUserId(userId)).thenReturn(List.of(vrWithNullCode, vrEmpty));

        ValidationReportDto dto = service.generateValidationReport(userId, true);

        assertNotNull(dto);
        assertEquals(1, dto.getTotalErrorCount());
        // null codes become "UNKNOWN" in implementation
        assertEquals("1", dto.getErrorCodeToErrorCount().get("UNKNOWN"));

        Map<String, Integer> perContent = dto.getErrorCountsPerContentTypes();
        assertNotNull(perContent);
        assertEquals(1, perContent.get("unknown").intValue());

        Map<String, Map<String, Integer>> perContentAnd = dto.getErrorCountsPerContentTypesAndErrorCode();
        assertNotNull(perContentAnd);
        assertTrue(perContentAnd.containsKey("unknown"));
        assertEquals(1, perContentAnd.get("unknown").get("UNKNOWN").intValue());
    }
}
