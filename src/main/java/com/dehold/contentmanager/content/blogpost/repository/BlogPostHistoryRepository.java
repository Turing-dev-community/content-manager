package com.dehold.contentmanager.content.blogpost.repository;

import com.dehold.contentmanager.content.blogpost.model.BlogPost;
import com.dehold.contentmanager.content.blogpost.model.BlogPostHistory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class BlogPostHistoryRepository {
    private final JdbcTemplate jdbcTemplate;

    public BlogPostHistoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void saveHistory(BlogPost blogPost, int versionNumber) {
        jdbcTemplate.update(
                "INSERT INTO blog_post_history (id, blog_post_id, title, content, version_number, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(),
                blogPost.getId(),
                blogPost.getTitle(),
                blogPost.getContent(),
                versionNumber,
                blogPost.getCreatedAt(),
                blogPost.getUpdatedAt()
        );
    }

    public List<BlogPostHistory> getHistoryByBlogPostId(UUID blogPostId) {
        return jdbcTemplate.query(
                "SELECT * FROM blog_post_history WHERE blog_post_id = ? ORDER BY version_number DESC",
                (rs, rowNum) -> {
                    BlogPostHistory history = new BlogPostHistory();
                    history.setId(UUID.fromString(rs.getString("id")));
                    history.setBlogPostId(UUID.fromString(rs.getString("blog_post_id")));
                    history.setTitle(rs.getString("title"));
                    history.setContent(rs.getString("content"));
                    history.setVersionNumber(rs.getInt("version_number"));
                    history.setCreatedAt(rs.getTimestamp("created_at").toInstant());
                    history.setUpdatedAt(rs.getTimestamp("updated_at").toInstant());
                    return history;
                },
                blogPostId
        );
    }

    public int getNextVersionNumber(UUID blogPostId) {
        Integer version = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(version_number), 0) FROM blog_post_history WHERE blog_post_id = ?",
                Integer.class,
                blogPostId
        );
        return version == null ? 1 : version;
    }
}
