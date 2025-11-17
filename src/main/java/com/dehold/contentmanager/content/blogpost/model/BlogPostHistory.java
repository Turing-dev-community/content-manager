package com.dehold.contentmanager.content.blogpost.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class BlogPostHistory {
    private UUID id;
    private UUID blogPostId;
    private String title;
    private String content;
    private int versionNumber;
    private Instant createdAt;
    private Instant updatedAt;

    public BlogPostHistory() {
    }

    public BlogPostHistory(UUID id, UUID blogPostId, String title, String content, int versionNumber, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.blogPostId = blogPostId;
        this.title = title;
        this.content = content;
        this.versionNumber = versionNumber;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getBlogPostId() {
        return blogPostId;
    }

    public void setBlogPostId(UUID blogPostId) {
        this.blogPostId = blogPostId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(int versionNumber) {
        this.versionNumber = versionNumber;
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

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        BlogPostHistory that = (BlogPostHistory) o;
        return versionNumber == that.versionNumber && Objects.equals(id, that.id) && Objects.equals(blogPostId, that.blogPostId) && Objects.equals(title, that.title) && Objects.equals(content, that.content) && Objects.equals(createdAt, that.createdAt) && Objects.equals(updatedAt, that.updatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, blogPostId, title, content, versionNumber, createdAt, updatedAt);
    }

    @Override
    public String toString() {
        return "BlogPostHistory{" +
                "id=" + id +
                ", blogPostId=" + blogPostId +
                ", title='" + title + '\'' +
                ", content='" + content + '\'' +
                ", versionNumber=" + versionNumber +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
