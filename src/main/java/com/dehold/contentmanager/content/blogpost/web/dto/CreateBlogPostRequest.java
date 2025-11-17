package com.dehold.contentmanager.content.blogpost.web.dto;

import com.dehold.contentmanager.content.blogpost.model.Comment;

import java.util.List;
import java.util.UUID;

public class CreateBlogPostRequest {

    private String title;
    private String content;
    private UUID userId;
    private List<Comment> comments;

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
}
