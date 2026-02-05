package com.dehold.contentmanager.validation.service;

import com.dehold.contentmanager.validation.web.dto.ValidationReportDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Component;

@Component
public class ValidationReportExporterUtility {

    private final ObjectMapper objectMapper;

    public ValidationReportExporterUtility() {
        this.objectMapper = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT); // pretty print
    }

    public ValidationReportExporterUtility(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public byte[] exportToJson(ValidationReportDto dto) {
        try {
            if(dto != null){
                return objectMapper.writeValueAsBytes(dto);
            } else {
                return "null".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to export ValidationReportDto to JSON", e);
        }
    }

    /* Add other export type methods as and when required */
}