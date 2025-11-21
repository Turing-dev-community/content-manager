package com.dehold.contentmanager.validation.web.dto;

import java.util.Map;

public class ValidationReportDto {
    int totalErrorCount;
    Map<String, String> errorCodeToErrorCount;

    // detailed fields
    private Map<String, Integer> errorCountsPerContentTypes;
    private Map<String, Map<String, Integer>> errorCountsPerContentTypesAndErrorCode;


    public ValidationReportDto(int totalErrorCount, Map<String, String> errorCodeToErrorCount) {
        this.totalErrorCount = totalErrorCount;
        this.errorCodeToErrorCount = errorCodeToErrorCount;
    }

    public ValidationReportDto() {}

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

    public Map<String, Integer> getErrorCountsPerContentTypes() {
        return errorCountsPerContentTypes;
    }

    public void setErrorCountsPerContentTypes(Map<String, Integer> errorCountsPerContentTypes) {
        this.errorCountsPerContentTypes = errorCountsPerContentTypes;
    }

    public Map<String, Map<String, Integer>> getErrorCountsPerContentTypesAndErrorCode() {
        return errorCountsPerContentTypesAndErrorCode;
    }

    public void setErrorCountsPerContentTypesAndErrorCode(Map<String, Map<String, Integer>> errorCountsPerContentTypesAndErrorCode) {
        this.errorCountsPerContentTypesAndErrorCode = errorCountsPerContentTypesAndErrorCode;
    }

}
