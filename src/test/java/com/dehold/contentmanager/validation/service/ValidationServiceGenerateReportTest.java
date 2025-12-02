package com.dehold.contentmanager.validation.service;

import com.dehold.contentmanager.content.blogpost.service.BlogPostService;
import com.dehold.contentmanager.content.customersupport.repository.SupportRequestRepository;
import com.dehold.contentmanager.content.customersupport.service.SupportResponseService;
import com.dehold.contentmanager.content.generic.service.GenericContentService;
import com.dehold.contentmanager.user.repository.UserRepository;
import com.dehold.contentmanager.validation.model.ValidationError;
import com.dehold.contentmanager.validation.model.ValidationResult;
import com.dehold.contentmanager.validation.pipeline.ValidationPipelineFactory;
import com.dehold.contentmanager.validation.repository.ValidationResultRepository;
import com.dehold.contentmanager.validation.web.dto.ValidationReportDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ValidationServiceGenerateReportTest {

    @Mock
    private ValidationResultRepository repository;

    @InjectMocks
    private ValidationServiceImpl service;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        service = new ValidationServiceImpl(
                repository,
                mock(ValidationPipelineFactory.class),
                mock(BlogPostService.class),
                mock(SupportResponseService.class),
                mock(SupportRequestRepository.class),
                mock(GenericContentService.class),
                mock(UserRepository.class)
        );
    }

    @Test
    void summaryMode_countsAllErrorsCorrectly_andDetailedFieldsAreNull() {
        UUID userId = UUID.randomUUID();

        ValidationError e1 = mock(ValidationError.class);
        when(e1.code()).thenReturn("LEN");

        ValidationError e2 = mock(ValidationError.class);
        when(e2.code()).thenReturn("PHONE");

        ValidationResult vr1 = mock(ValidationResult.class);
        when(vr1.getErrors()).thenReturn(List.of(e1));

        ValidationResult vr2 = mock(ValidationResult.class);
        when(vr2.getErrors()).thenReturn(List.of(e1, e2));

        when(repository.findByUserId(userId)).thenReturn(List.of(vr1, vr2));

        ValidationReportDto dto = service.generateValidationReport(userId, false);

        assertEquals(3, dto.getTotalErrorCount());
        assertEquals("2", dto.getErrorCodeToErrorCount().get("LEN"));
        assertEquals("1", dto.getErrorCodeToErrorCount().get("PHONE"));

        // As per review: detailed fields MUST be null in summary mode
        assertNull(dto.getErrorCountsPerContentTypes());
        assertNull(dto.getErrorCountsPerContentTypesAndErrorCode());
    }

    @Test
    void detailedMode_twoDifferentContentTypes_areCountedSeparately() {
        UUID userId = UUID.randomUUID();

        ValidationError eLen = mock(ValidationError.class);
        when(eLen.code()).thenReturn("LENGTH_VALIDATION_FAILED");

        ValidationError ePhone = mock(ValidationError.class);
        when(ePhone.code()).thenReturn("PHONE_NUMBER_FORBIDDEN_VALIDATION_FAILED");

        // BlogPost with 2 errors
        ValidationResult blogVr = mock(ValidationResult.class);
        when(blogVr.getContentType()).thenReturn("blogpost");
        when(blogVr.getErrors()).thenReturn(List.of(eLen, ePhone));

        // SupportRequest with 1 length error
        ValidationResult supportVr = mock(ValidationResult.class);
        when(supportVr.getContentType()).thenReturn("supportrequest");
        when(supportVr.getErrors()).thenReturn(List.of(eLen));

        when(repository.findByUserId(userId)).thenReturn(List.of(blogVr, supportVr));

        ValidationReportDto dto = service.generateValidationReport(userId, true);

        assertEquals(3, dto.getTotalErrorCount());

        // Global
        assertEquals("2", dto.getErrorCodeToErrorCount().get("LENGTH_VALIDATION_FAILED"));
        assertEquals("1", dto.getErrorCodeToErrorCount().get("PHONE_NUMBER_FORBIDDEN_VALIDATION_FAILED"));

        // Per content type
        Map<String, Integer> perType = dto.getErrorCountsPerContentTypes();
        assertEquals(2, perType.get("blogpost"));
        assertEquals(1, perType.get("supportrequest"));

        // Per content + error code
        Map<String, Map<String, Integer>> perTypeCode = dto.getErrorCountsPerContentTypesAndErrorCode();

        assertEquals(1, perTypeCode.get("blogpost").get("LENGTH_VALIDATION_FAILED"));
        assertEquals(1, perTypeCode.get("blogpost").get("PHONE_NUMBER_FORBIDDEN_VALIDATION_FAILED"));
        assertEquals(1, perTypeCode.get("supportrequest").get("LENGTH_VALIDATION_FAILED"));
    }

    @Test
    void nullUserId_returnsEmptySummary() {
        ValidationReportDto dto = service.generateValidationReport(null, false);
        assertEquals(0, dto.getTotalErrorCount());
        assertTrue(dto.getErrorCodeToErrorCount().isEmpty());
        assertNull(dto.getErrorCountsPerContentTypes());
        assertNull(dto.getErrorCountsPerContentTypesAndErrorCode());
    }

    @Test
    void emptyResults_returnZeroCounts_inDetailedMode() {
        UUID userId = UUID.randomUUID();
        when(repository.findByUserId(userId)).thenReturn(List.of());

        ValidationReportDto dto = service.generateValidationReport(userId, true);
        assertEquals(0, dto.getTotalErrorCount());
        assertTrue(dto.getErrorCodeToErrorCount().isEmpty());
        assertTrue(dto.getErrorCountsPerContentTypes().isEmpty());
        assertTrue(dto.getErrorCountsPerContentTypesAndErrorCode().isEmpty());
    }

    @Test
    void nullErrorCode_isIgnored_notCounted() {
        UUID userId = UUID.randomUUID();

        ValidationError eNull = mock(ValidationError.class);
        when(eNull.code()).thenReturn(null);

        ValidationResult vr = mock(ValidationResult.class);
        when(vr.getErrors()).thenReturn(List.of(eNull));
        when(vr.getContentType()).thenReturn("blogpost");

        when(repository.findByUserId(userId)).thenReturn(List.of(vr));

        ValidationReportDto dto = service.generateValidationReport(userId, true);

        // Null code error is ignored -> zero errors
        assertEquals(0, dto.getTotalErrorCount());
        assertTrue(dto.getErrorCodeToErrorCount().isEmpty());
        assertTrue(dto.getErrorCountsPerContentTypes().isEmpty());
        assertTrue(dto.getErrorCountsPerContentTypesAndErrorCode().isEmpty());
    }

    @Test
    void emptyErrorList_isIgnored() {
        UUID userId = UUID.randomUUID();

        ValidationResult vr = mock(ValidationResult.class);
        when(vr.getErrors()).thenReturn(List.of());
        when(vr.getContentType()).thenReturn("blogpost");

        when(repository.findByUserId(userId)).thenReturn(List.of(vr));

        ValidationReportDto dto = service.generateValidationReport(userId, true);

        assertEquals(0, dto.getTotalErrorCount());
        assertTrue(dto.getErrorCountsPerContentTypes().isEmpty());
        assertTrue(dto.getErrorCountsPerContentTypesAndErrorCode().isEmpty());
    }
}