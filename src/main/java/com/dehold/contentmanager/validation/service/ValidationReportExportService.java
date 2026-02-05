package com.dehold.contentmanager.validation.service;

import com.dehold.contentmanager.validation.web.dto.ValidationReportDto;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ValidationReportExportService {

    private final ValidationService validationService;
    private final ValidationReportExporterUtility exporterUtility;

    public ValidationReportExportService(ValidationService validationService,
                                         ValidationReportExporterUtility exporterUtility) {
        this.validationService = validationService;
        this.exporterUtility = exporterUtility;
    }

    public ExportResult exportReport(UUID userId, boolean detailed, String format) {
        ValidationReportDto report = validationService.generateValidationReport(userId, detailed);

        byte[] data;
        String fileName;

        switch (format) {
            case "json" -> {
                data = exporterUtility.exportToJson(report);
                fileName = "validation-report-" + userId + ".json";
            }
            default -> throw new IllegalArgumentException("Unsupported format: " + format);
        }
        return new ExportResult(fileName, data);
    }

    public record ExportResult(String fileName, byte[] data) {}
}