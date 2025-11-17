package com.dehold.contentmanager.content.blogpost.web;

import com.dehold.contentmanager.ContentManagerApplicationTests;
import com.dehold.contentmanager.content.blogpost.model.BlogPost;
import com.dehold.contentmanager.content.blogpost.model.BlogPostHistory;
import com.dehold.contentmanager.content.blogpost.repository.BlogPostHistoryRepository;
import com.dehold.contentmanager.content.blogpost.repository.BlogPostRepository;
import com.dehold.contentmanager.content.blogpost.service.BlogPostService;
import com.dehold.contentmanager.content.blogpost.web.dto.CreateBlogPostRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;


class BlogPostControllerHistoryRestoreTest extends ContentManagerApplicationTests {

    private static final UUID user1Id = UUID.fromString("06c4f0e4-20d7-4886-841b-ebe0ca3622a5");

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private BlogPostRepository blogPostRepository;

    @Autowired
    private BlogPostHistoryRepository blogPostHistoryRepository;

    @Autowired
    private BlogPostService blogPostService;

    @BeforeAll
    static void beforeAll(@Autowired JdbcTemplate jdbcTemplate) {
        jdbcTemplate.update("DELETE FROM \"user\" WHERE id = ?", user1Id);
        jdbcTemplate.update(
                "MERGE INTO \"user\" (id, alias, email, username, password, enabled, created_at, updated_at) " +
                        "KEY(id) VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                user1Id, "testuser1", "testuser1@example.com", "TestUser", "TestPassword", true
        );
    }

    @BeforeEach
    void cleanDb() {
        // remove blog_post_history first because of FK references
        jdbcTemplate.execute("TRUNCATE TABLE blog_post_history");
        jdbcTemplate.execute("TRUNCATE TABLE blog_post");
    }

    @Test
    void getHistory_forNewPost_returnsEmptyList() {
        // create a blog post via repository (or REST)
        BlogPost post = new BlogPost(UUID.randomUUID(), "Title A", "Content A", Instant.now(), Instant.now(), user1Id);
        blogPostRepository.createBlogPost(post);

        ResponseEntity<BlogPostHistory[]> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/blogposts/" + post.getId() + "/history",
                BlogPostHistory[].class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().length, "New post should have no history entries");
    }

    @Test
    void update_blogPostVersion_createsHistory_and_getHistoryReturnsIt() {
        // create a blog post
        BlogPost post = new BlogPost(UUID.randomUUID(), "Initial Title", "Initial Content", Instant.now(), Instant.now(), user1Id);
        blogPostRepository.createBlogPost(post);

        // call service method that archives current state and updates the post
        BlogPost updated = blogPostService.updateBlogPostVersion(post.getId(), "Updated Title", "Updated Content");

        // verify main post was updated
        BlogPost fromDb = blogPostRepository.getBlogPost(post.getId()).orElseThrow();
        assertEquals("Updated Title", fromDb.getTitle());
        assertEquals("Updated Content", fromDb.getContent());

        // GET history via REST endpoint and assert one entry (the archived previous state)
        ResponseEntity<BlogPostHistory[]> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/blogposts/" + post.getId() + "/history",
                BlogPostHistory[].class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        BlogPostHistory[] history = response.getBody();
        assertNotNull(history);
        assertEquals(1, history.length, "One history entry should be present after updateBlogPostVersion");

        BlogPostHistory h = history[0];
        assertEquals(post.getId(), h.getBlogPostId());
        assertEquals("Initial Title", h.getTitle());
        assertEquals("Initial Content", h.getContent());
        assertTrue(h.getVersionNumber() >= 1);
    }

    @Test
    void restoreVersion_whenVersionMissing_returns404() {
        // create a blog post
        BlogPost post = new BlogPost(UUID.randomUUID(), "Solo Title", "Solo Content", Instant.now(), Instant.now(), user1Id);
        blogPostRepository.createBlogPost(post);

        // Try to restore a non-existing version (e.g. 999)
        ResponseEntity<String> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/blogposts/" + post.getId() + "/restore/{version}",
                null,
                String.class,
                999
        );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("The entity BlogPostVersion") || response.getBody().toLowerCase().contains("does not exist"));
    }

    @Test
    void restoreVersion_successfullyRestoresVersion() {
        // CREATE a blog post
        UUID postId = UUID.randomUUID();
        BlogPost post = new BlogPost(
                postId,
                "Original Title",
                "Original Content",
                Instant.now(),
                Instant.now(),
                user1Id
        );
        blogPostRepository.createBlogPost(post);

        // UPDATE POST to create history entry
        blogPostService.updateBlogPostVersion(postId, "Updated Title", "Updated Content");

        // VERIFY history exists
        List<BlogPostHistory> history = blogPostHistoryRepository.getHistoryByBlogPostId(postId);
        assertEquals(1, history.size());
        int versionToRestore = history.get(0).getVersionNumber();

        // --- ACT: RESTORE via REST endpoint ---
        ResponseEntity<BlogPost> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/blogposts/" + postId + "/restore/{version}",
                null,
                BlogPost.class,
                versionToRestore
        );

        // ASSERT HTTP response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        BlogPost restored = response.getBody();
        assertNotNull(restored);

        // ASSERT restored content
        assertEquals("Original Title", restored.getTitle());
        assertEquals("Original Content", restored.getContent());

        // ASSERT DB reflects restored values
        BlogPost fromDb = blogPostRepository.getBlogPost(postId).orElseThrow();
        assertEquals("Original Title", fromDb.getTitle());
        assertEquals("Original Content", fromDb.getContent());
    }
}
