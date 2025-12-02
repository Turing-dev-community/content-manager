package com.dehold.contentmanager.validation.service;

import com.dehold.contentmanager.validation.web.dto.ValidationReportDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ValidationReportExportServiceTest {

    private ValidationService validationService;
    private ValidationReportExporterUtility jsonExporter;
    private ValidationReportExportService exportService;

    @BeforeEach
    void setUp() {
        validationService = mock(ValidationService.class);
        jsonExporter = mock(ValidationReportExporterUtility.class);
        exportService = new ValidationReportExportService(validationService, jsonExporter);
    }

    @Test
    void givenValidUserId_whenExport_thenFileNameAndBytesReturned() throws Exception {
        UUID userId = UUID.randomUUID();
        ValidationReportDto report = new ValidationReportDto(
                5, Map.of("ERR1", "5")
        );

        when(validationService.generateValidationReport(userId, true)).thenReturn(report);
        when(jsonExporter.exportToJson(report)).thenReturn("{\"ok\":true}".getBytes());

        var result = exportService.exportReport(userId, true, "json");

        assertNotNull(result);
        assertEquals("validation-report-" + userId + ".json", result.fileName());
        assertEquals("{\"ok\":true}", new String(result.data()));
    }
}
