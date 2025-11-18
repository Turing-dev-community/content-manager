package com.dehold.contentmanager.content.faqpage.model;

import java.util.Objects;

public class FaqItem {
    private String title;
    private String text;

    public FaqItem() {}

    public FaqItem(String title, String text) {
        this.title = title;
        this.text = text;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FaqItem faqItem = (FaqItem) o;
        return Objects.equals(title, faqItem.title) && Objects.equals(text, faqItem.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, text);
    }
}
