package com.dehold.contentmanager.content.blogpost.repository;

import com.dehold.contentmanager.content.blogpost.model.BlogPost;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class BlogPostRepository {

    private final JdbcTemplate jdbcTemplate;

    public BlogPostRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void createBlogPost(BlogPost blogPost) {
        jdbcTemplate.update(
                "INSERT INTO blog_post (id, title, content, created_at, updated_at, user_id) VALUES (?, ?, ?, ?, ?, ?)",
                blogPost.getId(),
                blogPost.getTitle(),
                blogPost.getContent(),
                blogPost.getCreatedAt(),
                blogPost.getUpdatedAt(),
                blogPost.getUserId()
        );
    }

    public Optional<BlogPost> getBlogPost(UUID id) {
        return jdbcTemplate.query(
                "SELECT * FROM blog_post WHERE id = ?",
                this::mapRowToBlogPost,
                id
        ).stream().findFirst();
    }

    public List<BlogPost> getAllBlogPosts() {
        return jdbcTemplate.query("SELECT * FROM blog_post", this::mapRowToBlogPost);
    }

    public List<BlogPost> getBlogPostsByUserId(UUID userId) {
        return jdbcTemplate.query(
                "SELECT * FROM blog_post WHERE user_id = ?",
                this::mapRowToBlogPost,
                userId
        );
    }

    public void updateBlogPost(BlogPost blogPost) {
        jdbcTemplate.update(
                "UPDATE blog_post SET title = ?, content = ?, updated_at = ? WHERE id = ?",
                blogPost.getTitle(),
                blogPost.getContent(),
                blogPost.getUpdatedAt(),
                blogPost.getId()
        );
    }

    public void deleteBlogPost(UUID id) {
        jdbcTemplate.update("DELETE FROM blog_post WHERE id = ?", id);
    }

    private BlogPost mapRowToBlogPost(ResultSet rs, int rowNum) throws SQLException {
        return new BlogPost(
                UUID.fromString(rs.getString("id")),
                rs.getString("title"),
                rs.getString("content"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant(),
                UUID.fromString(rs.getString("user_id"))
        );
    }

    public List<BlogPost> getPaginatedBlogPosts(int limit, int offset, UUID userId) {
        if (userId == null) {
            return jdbcTemplate.query(
                "SELECT * FROM blog_post LIMIT ? OFFSET ?",
                this::mapRowToBlogPost,
                limit, offset
            );
        } else {
            return jdbcTemplate.query(
                "SELECT * FROM blog_post WHERE user_id = ? LIMIT ? OFFSET ?",
                this::mapRowToBlogPost,
                userId, limit, offset
            );
        }
    }

    public long countBlogPosts(UUID userId) {
        if (userId == null) {
            return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM blog_post", Long.class);
        } else {
            return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM blog_post WHERE user_id = ?", Long.class, userId);
        }
    }
}
