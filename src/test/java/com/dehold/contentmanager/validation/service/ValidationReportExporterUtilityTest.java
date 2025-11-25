package com.dehold.contentmanager.validation.service;

import com.dehold.contentmanager.validation.web.dto.ValidationReportDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ValidationReportExporterUtilityTest {

    private ValidationReportExporterUtility exporter;
    private ObjectMapper mapperMock;

    @BeforeEach
    void setUp() {
        exporter = new ValidationReportExporterUtility();
    }


    @Test
    void givenValidReport_whenExport_thenJsonIsReturned() throws Exception {
        ValidationReportDto dto = new ValidationReportDto(
                3,
                Map.of("ERR1", "2", "ERR2", "1")
        );

        byte[] json = exporter.exportToJson(dto);

        assertNotNull(json);
        String result = new String(json);

        assertTrue(result.contains("\"totalErrorCount\" : 3"));
        assertTrue(result.contains("\"errorCodeToErrorCount\""));
        assertTrue(result.contains("\"ERR1\" : \"2\""));
    }

    @Test
    void givenNullReport_whenExport_thenEmptyJsonReturned() throws Exception {
        byte[] json = exporter.exportToJson(null);

        assertNotNull(json);
        assertEquals(0, json.length);
    }


}
