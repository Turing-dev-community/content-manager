package com.dehold.contentmanager.content.blogpost.model;

import com.dehold.contentmanager.content.Content;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BlogPost implements Content {

    private UUID id;
    private String title;
    private String content;
    private Instant createdAt;
    private Instant updatedAt;
    private UUID userId; // Foreign key to User
    private List<Comment> comments = new ArrayList<>();
    private boolean softDeleted;
    private Instant deletedAt;

    public BlogPost() {
    }

    public BlogPost(UUID id, String title, String content, Instant createdAt, Instant updatedAt, UUID userId) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.userId = userId;
    }

    public BlogPost(UUID id, String title, String content, Instant createdAt, Instant updatedAt, UUID userId, List<Comment> comments) {
        this(id, title, content, createdAt, updatedAt, userId);
        if (comments != null) this.comments = comments;
    }

    public BlogPost(UUID id, String title, String content, Instant createdAt, Instant updatedAt, UUID userId, List<Comment> comments, boolean softDeleted, Instant deletedAt) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.userId = userId;
        this.comments = comments;
        this.softDeleted = softDeleted;
        this.deletedAt = deletedAt;
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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

    public List<Comment> getComments() {
        return comments;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }

    public boolean isSoftDeleted() {
        return softDeleted;
    }

    public void setSoftDeleted(boolean softDeleted) {
        this.softDeleted = softDeleted;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }
}
