package com.dehold.contentmanager.validation.web.dto;

import java.util.Map;

public class ValidationReportDto {
    int totalErrorCount;
    Map<String, String> errorCodeToErrorCount;

    public ValidationReportDto(int totalErrorCount, Map<String, String> errorCodeToErrorCount) {
        this.totalErrorCount = totalErrorCount;
        this.errorCodeToErrorCount = errorCodeToErrorCount;
    }

    public int getTotalErrorCount() {
        return totalErrorCount;
    }

    public void setTotalErrorCount(int totalErrorCount) {
        this.totalErrorCount = totalErrorCount;
    }

    public Map<String, String> getErrorCodeToErrorCount() {
        return errorCodeToErrorCount;
    }

    public void setErrorCodeToErrorCount(Map<String, String> errorCodeToErrorCount) {
        this.errorCodeToErrorCount = errorCodeToErrorCount;
    }


}
