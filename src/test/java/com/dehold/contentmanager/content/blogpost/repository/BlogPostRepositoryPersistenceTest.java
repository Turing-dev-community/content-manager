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
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
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
        req.setUsername("TestUser"+ UUID.randomUUID());
        req.setPassword("TestPassword"+ UUID.randomUUID());
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

    @Test
    void softDelete_shouldMarkPostAsSoftDeleted() {
        UUID postId = UUID.randomUUID();

        BlogPost bp = new BlogPost(postId, "t", "c", Instant.now(), Instant.now(), userId);
        blogPostRepository.createBlogPost(bp);

        blogPostRepository.softDelete(postId);

        Boolean deleted = jdbcTemplate.queryForObject(
                "SELECT soft_deleted FROM blog_post WHERE id = ?",
                Boolean.class, postId);

        assertTrue(deleted);

        Instant deletedAt = jdbcTemplate.queryForObject(
                "SELECT deleted_at FROM blog_post WHERE id = ?",
                Instant.class, postId);

        assertNotNull(deletedAt);
    }

    @Test
    void getBlogPost_withoutIncludeSoftDeleted_shouldNotReturnSoftDeletedPost() {
        UUID id = UUID.randomUUID();
        BlogPost bp = new BlogPost(id, "t1", "c1", Instant.now(), Instant.now(), userId);
        blogPostRepository.createBlogPost(bp);

        blogPostRepository.softDelete(id);

        var result = blogPostRepository.getBlogPost(id, false);

        assertTrue(result.isEmpty());
    }

    @Test
    void getBlogPost_withIncludeSoftDeleted_shouldReturnSoftDeletedPost() {
        UUID id = UUID.randomUUID();
        BlogPost bp = new BlogPost(id, "t1", "c1", Instant.now(), Instant.now(), userId);
        blogPostRepository.createBlogPost(bp);

        blogPostRepository.softDelete(id);

        var result = blogPostRepository.getBlogPost(id, true);

        assertTrue(result.isPresent());
        assertTrue(result.get().isSoftDeleted());
    }

    @Test
    void getPaginatedBlogPosts_shouldReturnInAscendingOrderByCreatedAt() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        BlogPost older = new BlogPost(id1, "old", "oldC",
                Instant.now().minusSeconds(50), Instant.now(), userId);
        BlogPost newer = new BlogPost(id2, "new", "newC",
                Instant.now(), Instant.now(), userId);

        blogPostRepository.createBlogPost(older);
        blogPostRepository.createBlogPost(newer);

        List<BlogPost> result = blogPostRepository.getPaginatedBlogPosts(10, 0, userId, false);

        assertEquals(id1, result.get(0).getId());
        assertEquals(id2, result.get(1).getId());
    }

    @Test
    void getPaginatedBlogPosts_shouldExcludeSoftDeleted_whenFlagFalse() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        BlogPost active = new BlogPost(id1, "act", "body", Instant.now(), Instant.now(), userId);
        BlogPost deleted = new BlogPost(id2, "del", "body", Instant.now(), Instant.now(), userId);

        blogPostRepository.createBlogPost(active);
        blogPostRepository.createBlogPost(deleted);
        blogPostRepository.softDelete(id2);

        List<BlogPost> result = blogPostRepository.getPaginatedBlogPosts(10, 0, userId, false);

        assertEquals(1, result.size());
        assertEquals(id1, result.get(0).getId());
    }

    @Test
    void getPaginatedBlogPosts_shouldIncludeSoftDeleted_whenFlagTrue() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        BlogPost active = new BlogPost(id1, "A", "Body", Instant.now(), Instant.now(), userId);
        BlogPost deleted = new BlogPost(id2, "B", "Body", Instant.now(), Instant.now(), userId);

        blogPostRepository.createBlogPost(active);
        blogPostRepository.createBlogPost(deleted);
        blogPostRepository.softDelete(id2);

        List<BlogPost> result = blogPostRepository.getPaginatedBlogPosts(10, 0, userId, true);

        assertEquals(2, result.size());
    }



}