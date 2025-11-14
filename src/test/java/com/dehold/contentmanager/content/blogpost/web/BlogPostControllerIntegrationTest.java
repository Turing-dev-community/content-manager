package com.dehold.contentmanager.content.blogpost.web;

import com.dehold.contentmanager.content.blogpost.model.BlogPost;
import com.dehold.contentmanager.content.blogpost.repository.BlogPostRepository;
import com.dehold.contentmanager.content.blogpost.web.dto.CreateBlogPostRequest;
import com.dehold.contentmanager.content.blogpost.web.dto.UpdateBlogPostRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BlogPostControllerIntegrationTest {

    private static final UUID user1Id = UUID.fromString("06c4f0e4-20d7-4886-841b-ebe0ca3622a5");
    private static final UUID user2Id = UUID.fromString("514b7a57-39a7-4623-9db0-3fda971bf11f");

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private BlogPostRepository blogPostRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeAll
    static void setup(@Autowired JdbcTemplate jdbcTemplate) {
        jdbcTemplate.update("INSERT INTO \"user\" (id, alias, email, created_at, updated_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                user1Id, "testuser1", "testuser1@example.com");

        jdbcTemplate.update("INSERT INTO \"user\" (id, alias, email, created_at, updated_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                user2Id, "testuser2", "testuser2@example.com");
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

        ResponseEntity<BlogPost[]> response = restTemplate.getForEntity("http://localhost:" + port + "/api/blogposts" +
                "?userId=" + user1Id, BlogPost[].class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().length);
        assertEquals(blogPost1.getId(), response.getBody()[0].getId());
        assertEquals(blogPost2.getId(), response.getBody()[1].getId());
    }


    @Test
    void downloadExport_shouldReturnAttachmentWithJson() throws Exception {

        BlogPost bp = new BlogPost(UUID.randomUUID(), "DL Title", "DL Body", Instant.now(), Instant.now(), user1Id);
        blogPostRepository.createBlogPost(bp);

        String url = "http://localhost:" + port + "/api/blogposts/download/" + user1Id.toString();

        ResponseEntity<byte[]> response = restTemplate.getForEntity(url, byte[].class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        HttpHeaders headers = response.getHeaders();
        assertTrue(headers.containsKey(HttpHeaders.CONTENT_DISPOSITION));
        String cd = headers.getFirst(HttpHeaders.CONTENT_DISPOSITION);
        assertNotNull(cd);
        assertTrue(cd.contains("attachment"));
        assertTrue(cd.contains("export-" + user1Id + ".json"));

        assertEquals(MediaType.APPLICATION_JSON, headers.getContentType());
        byte[] body = response.getBody();
        assertNotNull(body);
        assertTrue(body.length > 0);

        // parse JSON and verify structure
        List blogPosts = objectMapper.readValue(body, List.class);
        //one blog post present
        assertNotNull(blogPosts);
        assertTrue(blogPosts.size() > 0);
    }
}
