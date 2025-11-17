package com.dehold.contentmanager.content.blogpost.repository;

import com.dehold.contentmanager.content.blogpost.model.BlogPost;
import com.dehold.contentmanager.content.blogpost.model.Comment;
import com.dehold.contentmanager.user.service.UserService;
import com.dehold.contentmanager.user.web.dto.CreateUserRequest;
import com.dehold.contentmanager.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class BlogPostRepositoryPersistenceTest {

    @Autowired
    private BlogPostRepository blogPostRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID userId;

    @BeforeEach
    void setup() {
        // use a unique email per setup to avoid DuplicateKey violations in H2
        CreateUserRequest req = new CreateUserRequest();
        req.setAlias("repo-user");
        String uniqueEmail = "repo-user+" + UUID.randomUUID() + "@example.com";
        req.setEmail(uniqueEmail);
        User u = userService.createUser(req);
        assertNotNull(u);
        userId = u.getId();
    }

    @Test
    void createBlogPost_shouldPersistBlogPostAndCommentsToDb() {
        UUID postId = UUID.randomUUID();
        BlogPost bp = new BlogPost(postId, "Title Persist", "Body Persist", Instant.now(), Instant.now(), userId);
        Comment c1 = new Comment(UUID.randomUUID(), userId, "Row comment 1", Instant.now(), Instant.now());
        Comment c2 = new Comment(UUID.randomUUID(), userId, "Row comment 2", Instant.now(), Instant.now());
        bp.setComments(List.of(c1, c2));

        blogPostRepository.createBlogPost(bp);

        Integer postCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM blog_post WHERE id = ?", Integer.class, postId);
        assertEquals(1, postCount);

        Integer commentCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM comments WHERE blog_post_id = ?", Integer.class, postId);
        assertEquals(2, commentCount);

        String text = jdbcTemplate.queryForObject("SELECT text FROM comments WHERE blog_post_id = ? ORDER BY created_at LIMIT 1", String.class, postId);
        assertEquals("Row comment 1", text);
    }

    @Test
    void getBlogPostsByUserId_shouldLoadCommentsFromDb() {
        UUID postId = UUID.randomUUID();
        BlogPost bp = new BlogPost(postId, "Title Load", "Body Load", Instant.now(), Instant.now(), userId);
        Comment c = new Comment(UUID.randomUUID(), userId, "LoadComment", Instant.now(), Instant.now());
        bp.setComments(List.of(c));
        blogPostRepository.createBlogPost(bp);

        var posts = blogPostRepository.getBlogPostsByUserId(userId);
        assertTrue(posts.size() >= 1);
        var found = posts.stream().filter(p -> p.getId().equals(postId)).findFirst();
        assertTrue(found.isPresent());
        assertEquals(1, found.get().getComments().size());
        assertEquals("LoadComment", found.get().getComments().get(0).getText());
    }
}