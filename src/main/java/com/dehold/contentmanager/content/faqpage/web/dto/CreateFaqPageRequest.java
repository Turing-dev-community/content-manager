package com.dehold.contentmanager.content.faqpage.web.dto;

import java.util.List;
import java.util.UUID;

public class CreateFaqPageRequest {
    private UUID userId;
    private String title;
    private String introduction;
    private List<FaqItemDto> faqItems;

    public CreateFaqPageRequest() {}

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getIntroduction() {
        return introduction;
    }

    public void setIntroduction(String introduction) {
        this.introduction = introduction;
    }

    public List<FaqItemDto> getFaqItems() {
        return faqItems;
    }

    public void setFaqItems(List<FaqItemDto> faqItems) {
        this.faqItems = faqItems;
    }
}
