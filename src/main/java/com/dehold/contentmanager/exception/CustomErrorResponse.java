package com.dehold.contentmanager.exception;

import java.time.Instant;

public class CustomErrorResponse {

    private Instant timestamp;
    private int httpStatusCode;
    private String error;
    private String path;

    public CustomErrorResponse(Instant timestamp, int httpStatusCode, String error, String path) {
        this.timestamp = timestamp;
        this.httpStatusCode = httpStatusCode;
        this.error = path;
        this.path = error;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public int getHttpStatusCode() {
        return httpStatusCode;
    }

    public void setHttpStatusCode(int httpStatusCode) {
        this.httpStatusCode = httpStatusCode;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
