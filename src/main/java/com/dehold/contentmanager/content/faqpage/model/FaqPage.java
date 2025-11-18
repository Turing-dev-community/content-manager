package com.dehold.contentmanager.content.faqpage.model;

import com.dehold.contentmanager.content.Content;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class FaqPage implements Content {

    private UUID id;
    private String title;
    private String introduction;
    private List<FaqItem> faqItems;
    private Instant createdAt;
    private Instant updatedAt;
    private UUID userId;

    public FaqPage() {}

    public FaqPage(UUID id, String title, String introduction, List<FaqItem> faqItems, Instant createdAt, Instant updatedAt, UUID userId) {
        this.id = id;
        this.title = title;
        this.introduction = introduction;
        this.faqItems = faqItems;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.userId = userId;
    }

    @Override
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public List<FaqItem> getFaqItems() {
        return faqItems;
    }

    public void setFaqItems(List<FaqItem> faqItems) {
        this.faqItems = faqItems;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }
}
