package com.dehold.contentmanager.content.blogpost.web;

import com.dehold.contentmanager.ContentManagerApplicationTests;
import com.dehold.contentmanager.content.blogpost.model.BlogPost;
import com.dehold.contentmanager.content.blogpost.model.Page;
import com.dehold.contentmanager.content.blogpost.repository.BlogPostRepository;
import com.dehold.contentmanager.content.blogpost.web.dto.CreateBlogPostRequest;
import com.dehold.contentmanager.content.blogpost.web.dto.UpdateBlogPostRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;


class BlogPostControllerIntegrationTest extends ContentManagerApplicationTests {

    private static final UUID user1Id = UUID.fromString("06c4f0e4-20d7-4886-841b-ebe0ca3622a5");
    private static final UUID user2Id = UUID.fromString("514b7a57-39a7-4623-9db0-3fda971bf11f");

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private BlogPostRepository blogPostRepository;

    @BeforeEach
    void cleanDatabase(@Autowired JdbcTemplate jdbcTemplate) {

        jdbcTemplate.update("DELETE FROM blog_post");
        jdbcTemplate.update("DELETE FROM \"user\"");

        jdbcTemplate.update("INSERT INTO \"user\" (id, alias, email, username, password, enabled, created_at, updated_at) VALUES (?, ?, ?, ?, ?, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                user1Id, "testuser1", "testuser1@example.com", "TestUser-" + UUID.randomUUID(), "TestPassword-" + UUID.randomUUID());

        jdbcTemplate.update("INSERT INTO \"user\" (id, alias, email, username, password, enabled, created_at, updated_at) VALUES (?, ?, ?, ?, ?, true,  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                user2Id, "testuser2", "testuser2@example.com", "TestUser-" + UUID.randomUUID(), "TestPassword-" + UUID.randomUUID());
    }

    @Test
    void createBlogPost_shouldReturnCreatedBlogPost() {
        CreateBlogPostRequest request = new CreateBlogPostRequest();
        request.setTitle("Integration Test Blog Post");
        request.setContent("This is a test blog post for integration testing.");

        ResponseEntity<BlogPost> response = restTemplate.postForEntity("http://localhost:" + port + "/api/blogposts", request, BlogPost.class);

        assertEquals(201, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(request.getTitle(), response.getBody().getTitle());
        assertEquals(request.getContent(), response.getBody().getContent());
    }

    @Test
    void getBlogPost_shouldReturnBlogPost() {
        BlogPost blogPost = new BlogPost(UUID.randomUUID(), "Integration Test Blog Post", "This is a test blog post " +
                "for integration testing.", Instant.now(), Instant.now(), user1Id);
        blogPostRepository.createBlogPost(blogPost);

        ResponseEntity<BlogPost> response = restTemplate.getForEntity("http://localhost:" + port + "/api/blogposts/" + blogPost.getId(), BlogPost.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(blogPost.getId(), response.getBody().getId());
    }

    @Test
    void updateBlogPost_shouldReturnUpdatedBlogPost() {
        BlogPost blogPost = new BlogPost(UUID.randomUUID(), "Old Title", "Old Content", Instant.now(), Instant.now(), user2Id);
        blogPostRepository.createBlogPost(blogPost);

        UpdateBlogPostRequest request = new UpdateBlogPostRequest();
        request.setTitle("Updated Title");
        request.setContent("Updated Content");

        HttpEntity<UpdateBlogPostRequest> entity = new HttpEntity<>(request);
        ResponseEntity<BlogPost> response = restTemplate.exchange("http://localhost:" + port + "/api/blogposts/" + blogPost.getId(), HttpMethod.PUT, entity, BlogPost.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(request.getTitle(), response.getBody().getTitle());
        assertEquals(request.getContent(), response.getBody().getContent());
    }

    @Test
    void deleteBlogPost_shouldDeleteBlogPost() {
        CreateBlogPostRequest createRequest = new CreateBlogPostRequest();
        createRequest.setTitle("Integration Test Blog Post");
        createRequest.setContent("This is a test blog post for integration testing.");

        ResponseEntity<BlogPost> createResponse = restTemplate.postForEntity("http://localhost:" + port + "/api/blogposts", createRequest, BlogPost.class);
        assertEquals(201, createResponse.getStatusCode().value());
        assertNotNull(createResponse.getBody());

        UUID blogPostId = createResponse.getBody().getId();

        restTemplate.delete("http://localhost:" + port + "/api/blogposts/" + blogPostId);

        ResponseEntity<BlogPost> getResponse = restTemplate.getForEntity("http://localhost:" + port + "/api/blogposts/" + blogPostId, BlogPost.class);
        assertEquals(404, getResponse.getStatusCode().value()); // Now correctly returns 404 Not Found
    }

    @Test
    void getBlogPost_shouldReturnNotFound() {
        UUID nonExistentId = UUID.randomUUID();

        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/api/blogposts/" + nonExistentId,
            String.class
        );

        assertEquals(404, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("The entity BlogPost with id " + nonExistentId + " does not exist"));
    }

    @Test
    void getBlogPostsByUser_shouldReturnBlogPostsForUser() {
        // Arrange: Create blog posts for user1
        BlogPost blogPost1 = new BlogPost(UUID.randomUUID(), "User1 Blog Post 1", "Content 1", Instant.now(), Instant.now(), user1Id);
        BlogPost blogPost2 = new BlogPost(UUID.randomUUID(), "User1 Blog Post 2", "Content 2", Instant.now(), Instant.now(), user1Id);
        blogPostRepository.createBlogPost(blogPost1);
        blogPostRepository.createBlogPost(blogPost2);

        ResponseEntity<Page<BlogPost>> response = restTemplate.exchange(
            "http://localhost:" + port + "/api/blogposts?userId=" + user1Id,
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<Page<BlogPost>>() {}
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().getContent().size());
        assertEquals(blogPost1.getId(), response.getBody().getContent().get(0).getId());
        assertEquals(blogPost2.getId(), response.getBody().getContent().get(1).getId());
    }

    @Test
    void getBlogPostsPaginated_shouldReturnFirstPageWithDefaults() {
        // Arrange: Create 25 blog posts for user1 to test multiple pages
        for (int i = 1; i <= 25; i++) {
            BlogPost blogPost = new BlogPost(UUID.randomUUID(), "Post " + i, "Content " + i, Instant.now(), Instant.now(), user1Id);
            blogPostRepository.createBlogPost(blogPost);
        }

        ResponseEntity<Page<BlogPost>> response = restTemplate.exchange(
            "http://localhost:" + port + "/api/blogposts?userId=" + user1Id,
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<Page<BlogPost>>() {}
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(20, response.getBody().getContent().size());  // Default size
        assertEquals(0, response.getBody().getPage());
        assertEquals(25, response.getBody().getTotalElements());
        assertEquals(2, response.getBody().getTotalPages());  // 25 / 20 = 2 pages
        assertFalse(response.getBody().isLast());
    }

    @Test
    void getBlogPostsPaginated_shouldRejectInvalidParams() {
        // Test invalid size > 100
        ResponseEntity<String> responseSize = restTemplate.exchange(
            "http://localhost:" + port + "/api/blogposts?page=0&size=200",
            HttpMethod.GET,
            null,
            String.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, responseSize.getStatusCode());
        assertTrue(responseSize.getBody().contains("Size must be between 1 and 100"));

        // Test invalid page < 0
        ResponseEntity<String> responsePage = restTemplate.exchange(
            "http://localhost:" + port + "/api/blogposts?page=-1&size=20",
            HttpMethod.GET,
            null,
            String.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, responsePage.getStatusCode());
        assertTrue(responsePage.getBody().contains("Page must be non-negative"));
    }

    @Test
    void getBlogPostsPaginated_shouldReturnLastPage() {
        // Arrange: Create 25 blog posts for user1
        for (int i = 1; i <= 25; i++) {
            BlogPost blogPost = new BlogPost(UUID.randomUUID(), "Post " + i, "Content " + i, Instant.now(), Instant.now(), user1Id);
            blogPostRepository.createBlogPost(blogPost);
        }

        ResponseEntity<Page<BlogPost>> response = restTemplate.exchange(
            "http://localhost:" + port + "/api/blogposts?page=2&size=10",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<Page<BlogPost>>() {}
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(5, response.getBody().getContent().size());  // Last page has 5 items (25 % 10 = 5)
        assertEquals(2, response.getBody().getPage());
        assertEquals(10, response.getBody().getSize());
        assertEquals(25, response.getBody().getTotalElements());
        assertEquals(3, response.getBody().getTotalPages());
        assertTrue(response.getBody().isLast());
    }

    @Test
    void getBlogPostsPaginated_shouldFilterByUserIdAndPaginate() {
        // Arrange: Create 15 blog posts for user1, 10 for user2
        for (int i = 1; i <= 15; i++) {
            BlogPost blogPost = new BlogPost(UUID.randomUUID(), "User1 Post " + i, "Content " + i, Instant.now(), Instant.now(), user1Id);
            blogPostRepository.createBlogPost(blogPost);
        }
        for (int i = 1; i <= 10; i++) {
            BlogPost blogPost = new BlogPost(UUID.randomUUID(), "User2 Post " + i, "Content " + i, Instant.now(), Instant.now(), user2Id);
            blogPostRepository.createBlogPost(blogPost);
        }

        ResponseEntity<Page<BlogPost>> response = restTemplate.exchange(
            "http://localhost:" + port + "/api/blogposts?userId=" + user1Id + "&page=0&size=10",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<Page<BlogPost>>() {}
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(10, response.getBody().getContent().size());
        assertEquals(15, response.getBody().getTotalElements());
        assertEquals(2, response.getBody().getTotalPages());    
        assertFalse(response.getBody().isLast());
        // Check all posts belong to user1
        for (BlogPost post : response.getBody().getContent()) {
            assertEquals(user1Id, post.getUserId());
        }
    }

    @Test
    void getBlogPostsPaginated_shouldReturnSecondPageWithCustomSize() {
        // Arrange: Create 25 blog posts for user1 to test multiple pages
        for (int i = 1; i <= 25; i++) {
            BlogPost blogPost = new BlogPost(UUID.randomUUID(), "Post " + i, "Content " + i, Instant.now(), Instant.now(), user1Id);
            blogPostRepository.createBlogPost(blogPost);
        }

        ResponseEntity<Page<BlogPost>> response = restTemplate.exchange(
            "http://localhost:" + port + "/api/blogposts?page=1&size=10",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<Page<BlogPost>>() {}
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(10, response.getBody().getContent().size());
        assertEquals(1, response.getBody().getPage());
        assertEquals(10, response.getBody().getSize());
        assertEquals(25, response.getBody().getTotalElements());
        assertEquals(3, response.getBody().getTotalPages());  // 25 / 10 = 3 pages
        assertFalse(response.getBody().isLast());
    }
}
