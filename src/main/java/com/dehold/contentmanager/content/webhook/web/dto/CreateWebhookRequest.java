package com.dehold.contentmanager.content.webhook.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class CreateWebhookRequest {

    @NotBlank(message = "URL is required")
    @Pattern(
        regexp = "^https?://[a-zA-Z0-9.-]+(\\.[a-zA-Z0-9.-]+)*(:\\d+)?(/.*)?$",
        message = "Please enter a valid URL (e.g. https://example.com/webhook or http://localhost:8080/hook)"
    )
    private String url;

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
   
}