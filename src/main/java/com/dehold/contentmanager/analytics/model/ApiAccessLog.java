package com.dehold.contentmanager.analytics.model;

import java.time.Instant;
import java.util.UUID;

public class ApiAccessLog {
    private UUID id;
    private String url;
    private Instant timestamp;

    public ApiAccessLog() {
    }

    public ApiAccessLog(UUID id, String url, Instant timestamp) {
        this.id = id;
        this.url = url;
        this.timestamp = timestamp;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}

