package com.dehold.contentmanager.content.blogpost.repository;

import com.dehold.contentmanager.content.blogpost.model.BlogPost;
import com.dehold.contentmanager.content.blogpost.model.Comment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
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
            "INSERT INTO blog_post (id, title, content, created_at, updated_at, state, user_id) VALUES (?, ?, ?, ?, ?, ?, ?)",
            blogPost.getId(),
            blogPost.getTitle(),
            blogPost.getContent(),
            blogPost.getCreatedAt(),
            blogPost.getUpdatedAt(),
            blogPost.getState() == null ? "DRAFT" : blogPost.getState().name(),
            blogPost.getUserId()
        );
        if (blogPost.getComments() != null) {
            for (Comment c : blogPost.getComments()) {
                insertComment(blogPost.getId(), c);
            }
        }
    }

    public Optional<BlogPost> getBlogPost(UUID id) {
        return jdbcTemplate.query(
                "SELECT * FROM blog_post WHERE id = ?",
                this::mapRowToBlogPost,
                id
        ).stream().findFirst().map(bp -> {
            bp.setComments(loadCommentsForPost(bp.getId()));
            return bp;
        });
    }

    public List<BlogPost> getAllBlogPosts() {
        List<BlogPost> posts = jdbcTemplate.query("SELECT * FROM blog_post", this::mapRowToBlogPost);
        for (BlogPost p : posts) {
            p.setComments(loadCommentsForPost(p.getId()));
        }
        return posts;
    }

    public List<BlogPost> getBlogPostsByUserId(UUID userId) {
        List<BlogPost> posts = jdbcTemplate.query(
                "SELECT * FROM blog_post WHERE user_id = ?",
                this::mapRowToBlogPost,
                userId
        );
        for (BlogPost p : posts) {
            p.setComments(loadCommentsForPost(p.getId()));
        }
        return posts;
    }

    public void updateBlogPost(BlogPost blogPost) {
        jdbcTemplate.update(
                "UPDATE blog_post SET title = ?, content = ?, updated_at = ?, state = ? WHERE id = ?",
                blogPost.getTitle(),
                blogPost.getContent(),
                blogPost.getUpdatedAt(),
                blogPost.getState() == null ? "DRAFT" : blogPost.getState().name(),
                blogPost.getId()
        );
    }

    public void deleteBlogPost(UUID id) {
        jdbcTemplate.update("DELETE FROM blog_post WHERE id = ?", id);
    }

    private BlogPost mapRowToBlogPost(ResultSet rs, int rowNum) throws SQLException {

        BlogPost bp = new BlogPost(
                UUID.fromString(rs.getString("id")),
                rs.getString("title"),
                rs.getString("content"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant(),
                UUID.fromString(rs.getString("user_id"))
        );

        // --- restore persisted state if present in the result set ---
        try {
            String stateStr = rs.getString("state");
            if (stateStr != null && !stateStr.isBlank()) {
                try {
                    bp.setState(BlogPost.State.valueOf(stateStr));
                } catch (IllegalArgumentException e) {
                    // unknown state stored in DB — fallback to DRAFT
                    bp.setState(BlogPost.State.DRAFT);
                }
            }
        } catch (SQLException ignored) {
            // Column might not exist in older schemas — keep default DRAFT
        }
        // ------- SAFE soft-delete handling (optional columns) --------
        try {
            boolean softDeleted = rs.getBoolean("soft_deleted");
            bp.setSoftDeleted(softDeleted);

            if (softDeleted) {
                var deletedAtTs = rs.getTimestamp("deleted_at");
                bp.setDeletedAt(deletedAtTs != null ? deletedAtTs.toInstant() : null);
            }
        } catch (SQLException ignored) {
            // Column doesn't exist in query or schema → default values
            bp.setSoftDeleted(false);
            bp.setDeletedAt(null);
        }

        return bp;
    }


    private Comment mapRowToComment(ResultSet rs, int rowNum) throws SQLException {
        return new Comment(
                UUID.fromString(rs.getString("id")),
                UUID.fromString(rs.getString("user_id")),
                rs.getString("text"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()
        );
    }

    private List<Comment> loadCommentsForPost(UUID postId) {
        List<Comment> comments = jdbcTemplate.query(
                "SELECT * FROM comments WHERE blog_post_id = ? ORDER BY created_at ASC",
                this::mapRowToComment,
                postId
        );
        return comments == null ? new ArrayList<>() : comments;
    }

    private void insertComment(UUID blogPostId, Comment c) {
        UUID cid = c.getId() == null ? UUID.randomUUID() : c.getId();
        Instant created = c.getCreatedAt() == null ? Instant.now() : c.getCreatedAt();
        Instant updated = c.getUpdatedAt() == null ? created : c.getUpdatedAt();

        jdbcTemplate.update(
                "INSERT INTO comments (id, blog_post_id, user_id, text, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)",
                cid,
                blogPostId,
                c.getUserId(),
                c.getText(),
                created,
                updated
        );
        c.setId(cid);
        c.setCreatedAt(created);
        c.setUpdatedAt(updated);
    }

    public List<BlogPost> getPaginatedBlogPosts(int limit, int offset, UUID userId) {
        List<BlogPost> posts;
        if (userId == null) {
            posts = jdbcTemplate.query(
                "SELECT * FROM blog_post LIMIT ? OFFSET ?",
                this::mapRowToBlogPost,
                limit, offset
            );
        } else {
            posts = jdbcTemplate.query(
                "SELECT * FROM blog_post WHERE user_id = ? LIMIT ? OFFSET ?",
                this::mapRowToBlogPost,
                userId, limit, offset
            );
        }
        for (BlogPost p : posts) {
            p.setComments(loadCommentsForPost(p.getId()));
        }
        return posts;
    }

    public long countBlogPosts(UUID userId) {
        if (userId == null) {
            return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM blog_post", Long.class);
        } else {
            return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM blog_post WHERE user_id = ?", Long.class, userId);
        }
    }

    public List<UUID> searchByTerm(String term) {
        if (term == null || term.trim().isEmpty()) {
            return List.of();
        }
    
        String sql = """
            SELECT id FROM blog_post
            WHERE title LIKE ? OR content LIKE ?
            """;
    
        String pattern = "%" + term.trim() + "%";
        return jdbcTemplate.queryForList(sql, UUID.class, pattern, pattern);
    }

    public void softDelete(UUID id) {
        jdbcTemplate.update(
                "UPDATE blog_post SET soft_deleted = TRUE, deleted_at = ? WHERE id = ?",
                Instant.now(), id
        );
    }

    public Optional<BlogPost> getBlogPost(UUID id, boolean includeSoftDeleted) {
        String sql = includeSoftDeleted ?
                "SELECT * FROM blog_post WHERE id = ?" :
                "SELECT * FROM blog_post WHERE id = ? AND soft_deleted = FALSE";

        return jdbcTemplate.query(sql, this::mapRowToBlogPost, id)
                .stream().findFirst()
                .map(bp -> {
                    bp.setComments(loadCommentsForPost(bp.getId()));
                    return bp;
                });
    }

    public List<BlogPost> getPaginatedBlogPosts(int limit, int offset, UUID userId, boolean includeSoftDeleted) {
        StringBuilder sql = new StringBuilder("SELECT * FROM blog_post WHERE 1=1");

        if (userId != null) {
            sql.append(" AND user_id = '").append(userId).append("'");
        }
        if (!includeSoftDeleted) {
            sql.append(" AND soft_deleted = false");
        }

        sql.append(" ORDER BY created_at ASC LIMIT ? OFFSET ?");

        List<BlogPost> posts = jdbcTemplate.query(sql.toString(), this::mapRowToBlogPost, limit, offset);
        posts.forEach(p -> p.setComments(loadCommentsForPost(p.getId())));
        return posts;
    }

    public long countBlogPosts(UUID userId, boolean includeSoftDeleted) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM blog_post WHERE 1=1");

        if (userId != null) {
            sql.append(" AND user_id = '").append(userId).append("'");
        }
        if (!includeSoftDeleted) {
            sql.append(" AND soft_deleted = false");
        }

        return jdbcTemplate.queryForObject(sql.toString(), Long.class);
    }

}
