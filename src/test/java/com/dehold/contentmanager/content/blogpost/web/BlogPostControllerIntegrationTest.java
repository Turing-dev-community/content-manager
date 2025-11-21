package com.dehold.contentmanager.content.blogpost.web;

import com.dehold.contentmanager.ContentManagerApplicationTests;
import com.dehold.contentmanager.content.blogpost.export.ExportResponse;
import com.dehold.contentmanager.content.blogpost.model.BlogPost;
import com.dehold.contentmanager.content.blogpost.model.Page;
import com.dehold.contentmanager.content.blogpost.repository.BlogPostRepository;
import com.dehold.contentmanager.content.blogpost.web.dto.BlogPostSearchResponse;
import com.dehold.contentmanager.content.blogpost.web.dto.CreateBlogPostRequest;
import com.dehold.contentmanager.content.blogpost.web.dto.UpdateBlogPostRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cache.CacheManager;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;


import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;


class BlogPostControllerIntegrationTest extends ContentManagerApplicationTests {

    private static final UUID user1Id = UUID.fromString("06c4f0e4-20d7-4886-841b-ebe0ca3622a5");
    private static final UUID user2Id = UUID.fromString("514b7a57-39a7-4623-9db0-3fda971bf11f");
    private static final UUID user3Id = UUID.randomUUID();

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private BlogPostRepository blogPostRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void cleanupDbAndSetupUsers(@Autowired JdbcTemplate jdbcTemplate) {

        cacheManager.getCacheNames().forEach(name ->
            cacheManager.getCache(name).clear()
        );

        jdbcTemplate.update("DELETE FROM blog_post");
        jdbcTemplate.update("DELETE FROM \"user\"");

        jdbcTemplate.update("INSERT INTO \"user\" (id, alias, email, username, password, enabled, created_at, updated_at) VALUES (?, ?, ?, ?, ?, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                user1Id, "testuser1", "testuser1@example.com", "TestUser-" + UUID.randomUUID(), "TestPassword-" + UUID.randomUUID());

        jdbcTemplate.update("INSERT INTO \"user\" (id, alias, email, username, password, enabled, created_at, updated_at) VALUES (?, ?, ?, ?, ?, true,  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                user2Id, "testuser2", "testuser2@example.com", "TestUser-" + UUID.randomUUID(), "TestPassword-" + UUID.randomUUID());

        jdbcTemplate.update("INSERT INTO \"user\" (id, alias, email, username, password, enabled, created_at, updated_at) VALUES (?, ?, ?, ?, ?, true,  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                user3Id, "testuser3", "testuser3@example.com", "TestUser-" + UUID.randomUUID(), "TestPassword-" + UUID.randomUUID());
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

    @Test
    void downloadExport_shouldReturnAttachmentHeaders() throws Exception {
        // create a blog post for this user
        BlogPost bp = new BlogPost(UUID.randomUUID(), "DL Title", "DL Body",
                Instant.now(), Instant.now(), user1Id);
        blogPostRepository.createBlogPost(bp);

        String url = "http://localhost:" + port + "/api/blogposts/download/" + user1Id;

        ResponseEntity<byte[]> response = restTemplate.getForEntity(url, byte[].class);

        // if feature not implemented this assert will fail (404 or other)
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Expected HTTP 200 from download endpoint");

        HttpHeaders headers = response.getHeaders();
        assertTrue(headers.containsKey(HttpHeaders.CONTENT_DISPOSITION), "Missing Content-Disposition header");

        String cd = headers.getFirst(HttpHeaders.CONTENT_DISPOSITION);
        assertNotNull(cd);

        // Assert it indicates attachment and contains a filename token matching export-*.json
        assertTrue(cd.toLowerCase().contains("attachment"), "Content-Disposition must indicate attachment");

        // filename may be formatted differently by implementations; assert a flexible pattern:
        // must contain "export-" and end with ".json" in the header value
        assertTrue(cd.contains("export-") && cd.toLowerCase().contains(".json"),
                "Content-Disposition filename should follow export-*.json pattern");

        // Content-Type check (explicit per issue)
        assertEquals(MediaType.APPLICATION_JSON, headers.getContentType(), "Content-Type must be application/json");
    }

    @Test
    void downloadExport_shouldReturnNonEmptyJsonFile() throws Exception {
        BlogPost bp = new BlogPost(UUID.randomUUID(), "DL Title 2", "DL Body 2",
                Instant.now(), Instant.now(), user1Id);
        blogPostRepository.createBlogPost(bp);

        String url = "http://localhost:" + port + "/api/blogposts/download/" + user1Id;

        ResponseEntity<byte[]> response = restTemplate.getForEntity(url, byte[].class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        byte[] body = response.getBody();
        assertNotNull(body, "Response body must not be null");
        assertTrue(body.length > 0, "Downloaded JSON must not be empty");
    }

    @Test
    void downloadExport_shouldContainBlogPostsInJsonArray() throws Exception {
        BlogPost bp = new BlogPost(UUID.randomUUID(), "DL Title 3", "DL Body 3",
                Instant.now(), Instant.now(), user1Id);
        blogPostRepository.createBlogPost(bp);

        String url = "http://localhost:" + port + "/api/blogposts/download/" + user1Id;

        ResponseEntity<byte[]> response = restTemplate.getForEntity(url, byte[].class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        byte[] body = response.getBody();
        assertNotNull(body);

        // parse ExportResponse (JSON object) and check its blogPosts array
        ExportResponse exportResp = objectMapper.readValue(body, ExportResponse.class);
        assertNotNull(exportResp);
        assertNotNull(exportResp.getBlogPosts());
        assertTrue(exportResp.getBlogPosts().size() > 0, "Exported blogPosts array must contain at least one blog post");
    }

    @Test
    void downloadExport_shouldContainBlogPostsInJsonArray_andValidateContent() throws Exception {
        // create a blog post for this user
        UUID createdId = UUID.randomUUID();
        BlogPost bp = new BlogPost(createdId, "DL Title 3", "DL Body 3",
                Instant.now(), Instant.now(), user1Id);
        blogPostRepository.createBlogPost(bp);

        String url = "http://localhost:" + port + "/api/blogposts/download/" + user1Id;

        ResponseEntity<byte[]> response = restTemplate.getForEntity(url, byte[].class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        byte[] body = response.getBody();
        assertNotNull(body);

        ExportResponse exportResp = objectMapper.readValue(body, ExportResponse.class);
        assertNotNull(exportResp);
        assertNotNull(exportResp.getBlogPosts());
        assertTrue(exportResp.getBlogPosts().size() > 0, "Exported blogPosts array must contain at least one blog post");

        // Inspect first BlogPost DTO (as Map) to assert fields are present if you prefer dynamic checks:
        Object first = exportResp.getBlogPosts().get(0);
        assertNotNull(first);
        // We can map it back to BlogPost class
        // ensure expected fields exist by converting first element to JSON then to Map (optional)
        // but simpler: assert the exported blogPosts contain an entry with the same id
        boolean hasMatch = exportResp.getBlogPosts().stream().anyMatch(p -> createdId.equals(p.getId()));
        assertTrue(hasMatch, "At least one exported blog post must have the expected id");
    }

    @Test
    void downloadExport_json_shouldReturnAttachmentAndContainBlogPost() throws Exception {
        // persist a blog post for the user
        UUID postId = UUID.randomUUID();
        BlogPost bp = new BlogPost(postId, "JSON Title", "JSON Body", Instant.now(), Instant.now(), user3Id);
        blogPostRepository.createBlogPost(bp);

        String url = "http://localhost:" + port + "/api/blogposts/download/" + user3Id + "?format=json";
        ResponseEntity<byte[]> response = restTemplate.getForEntity(url, byte[].class);

        assertEquals(HttpStatus.OK, response.getStatusCode(), "Expected 200 OK");
        HttpHeaders headers = response.getHeaders();
        assertEquals(MediaType.APPLICATION_JSON, headers.getContentType(), "Expected application/json");

        String cd = headers.getFirst(HttpHeaders.CONTENT_DISPOSITION);
        assertNotNull(cd, "Content-Disposition header required");
        assertTrue(cd.toLowerCase().contains("attachment"));
        assertTrue(cd.contains("export-"));

        byte[] body = response.getBody();
        assertNotNull(body, "Response body must not be null");

        // parse ExportResponse (JSON object) and assert blogPosts contains our post
        ExportResponse exportResp = objectMapper.readValue(body, ExportResponse.class);
        assertNotNull(exportResp);
        assertNotNull(exportResp.getBlogPosts());
        assertTrue(exportResp.getBlogPosts().size() > 0);

        boolean match = exportResp.getBlogPosts().stream()
                .anyMatch(p -> postId.equals(p.getId()) && "JSON Title".equals(p.getTitle()) && user3Id.equals(p.getUserId()));
        assertTrue(match, "Exported JSON must contain the persisted blog post with correct fields");
    }

    @Test
    void downloadExport_csv_shouldReturnCsvAttachmentAndContainBlogPost() throws Exception {
        UUID postId = UUID.randomUUID();
        BlogPost bp = new BlogPost(postId, "CSV, Title \"with quotes\"", "CSV Body\nwith newline", Instant.now(), Instant.now(), user3Id);
        blogPostRepository.createBlogPost(bp);

        String url = "http://localhost:" + port + "/api/blogposts/download/" + user3Id + "?format=csv";
        ResponseEntity<byte[]> response = restTemplate.getForEntity(url, byte[].class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        HttpHeaders headers = response.getHeaders();
        assertEquals(MediaType.valueOf("text/csv"), headers.getContentType(), "Expected text/csv");
        String cd = headers.getFirst(HttpHeaders.CONTENT_DISPOSITION);
        assertNotNull(cd);
        assertTrue(cd.toLowerCase().contains("attachment"));
        assertTrue(cd.contains("export-"));

        byte[] body = response.getBody();
        assertNotNull(body);
        String csv = new String(body, StandardCharsets.UTF_8);

        // CSV now starts with a section marker line "# BlogPosts", then the header line.
        String[] lines = csv.split("\\r?\\n");
        assertTrue(lines.length >= 3, "CSV should contain section line, header, and at least one data row");

        // line 0 is the section label
        assertEquals("# BlogPosts", lines[0]);
        // line 1 should be the header
        assertEquals("\"id\",\"title\",\"content\",\"createdAt\",\"updatedAt\",\"userId\"", lines[1]);

        // subsequent lines include the data row(s)
        // Check that the CSV contains the escaped title and the userId somewhere
        assertTrue(csv.contains("\"CSV, Title \"\"with quotes\"\"\"") || csv.contains("CSV, Title"), "CSV must contain escaped/quoted title");
        // ensure userId appears in CSV
        assertTrue(csv.contains(user3Id.toString()), "CSV must include the userId for the blog post");
    }

    @Test
    void downloadExport_defaultWithoutFormat_shouldReturnJson() throws Exception {
        BlogPost bp = new BlogPost(UUID.randomUUID(), "DEF Title", "DEF Body", Instant.now(), Instant.now(), user3Id);
        blogPostRepository.createBlogPost(bp);

        String url = "http://localhost:" + port + "/api/blogposts/download/" + user3Id;
        ResponseEntity<byte[]> response = restTemplate.getForEntity(url, byte[].class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        HttpHeaders headers = response.getHeaders();
        assertEquals(MediaType.APPLICATION_JSON, headers.getContentType());
        byte[] body = response.getBody();
        assertNotNull(body);
        // ensure it's valid JSON array
        Object parsed = objectMapper.readValue(body, Object.class);
        assertNotNull(parsed);
    }

    @Test
    void downloadExport_formatParam_caseInsensitive_shouldReturnCsv() throws Exception {
        BlogPost bp = new BlogPost(UUID.randomUUID(), "CASE Title", "CASE Body", Instant.now(), Instant.now(), user3Id);
        blogPostRepository.createBlogPost(bp);

        // uppercase CSV param
        String url = "http://localhost:" + port + "/api/blogposts/download/" + user3Id + "?format=CSV";
        ResponseEntity<byte[]> response = restTemplate.getForEntity(url, byte[].class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.valueOf("text/csv"), response.getHeaders().getContentType());
    }

    @Test
    void downloadExport_emptyUser_shouldReturnEmptyJsonArray_andCsvHeaderOnly() throws Exception {
        // JSON: should be an ExportResponse with empty blogPosts list
        String urlJson = "http://localhost:" + port + "/api/blogposts/download/" + user3Id + "?format=json";
        ResponseEntity<byte[]> respJson = restTemplate.getForEntity(urlJson, byte[].class);
        assertEquals(HttpStatus.OK, respJson.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, respJson.getHeaders().getContentType());

        // parse ExportResponse (object) instead of List
        ExportResponse exportResp = objectMapper.readValue(respJson.getBody(), ExportResponse.class);
        assertNotNull(exportResp);
        assertNotNull(exportResp.getBlogPosts());
        assertEquals(0, exportResp.getBlogPosts().size(), "Expected empty blogPosts array for user with no blog posts");

        // CSV: should contain header line but no blog-post data rows
        String urlCsv = "http://localhost:" + port + "/api/blogposts/download/" + user3Id + "?format=csv";
        ResponseEntity<byte[]> respCsv = restTemplate.getForEntity(urlCsv, byte[].class);
        assertEquals(HttpStatus.OK, respCsv.getStatusCode());
        assertEquals(MediaType.valueOf("text/csv"), respCsv.getHeaders().getContentType());
        String csv = new String(respCsv.getBody(), StandardCharsets.UTF_8);
        String[] lines = csv.split("\\r?\\n");
        // header line present, but no following data line for BlogPosts section
        assertTrue(lines.length >= 2);
        assertEquals("# BlogPosts", lines[0]);
        assertEquals("\"id\",\"title\",\"content\",\"createdAt\",\"updatedAt\",\"userId\"", lines[1]);
        // either only header or header + empty line (no data row)
        assertTrue(lines.length == 2 || (lines.length >= 3 && (lines[2].isEmpty() || lines[2].startsWith("# SupportRequests"))));
    }


    @Test
    void downloadExport_shouldIncludeFilenameExtension_inContentDisposition() throws Exception {
        UUID postId = UUID.randomUUID();
        BlogPost bp = new BlogPost(postId, "EXT Title", "EXT Body", Instant.now(), Instant.now(), user3Id);
        blogPostRepository.createBlogPost(bp);

        // JSON case
        String urlJson = "http://localhost:" + port + "/api/blogposts/download/" + user3Id + "?format=json";
        ResponseEntity<byte[]> respJson = restTemplate.getForEntity(urlJson, byte[].class);
        assertEquals(HttpStatus.OK, respJson.getStatusCode());
        String cdJson = respJson.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        assertNotNull(cdJson);
        assertTrue(cdJson.toLowerCase().contains("attachment"));
        assertTrue(cdJson.contains("export-"), "Content-Disposition must contain 'export-'");
        assertTrue(cdJson.toLowerCase().contains(".json"), "Filename must contain .json extension");

        // CSV case
        String urlCsv = "http://localhost:" + port + "/api/blogposts/download/" + user3Id + "?format=csv";
        ResponseEntity<byte[]> respCsv = restTemplate.getForEntity(urlCsv, byte[].class);
        assertEquals(HttpStatus.OK, respCsv.getStatusCode());
        String cdCsv = respCsv.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        assertNotNull(cdCsv);
        assertTrue(cdCsv.toLowerCase().contains("attachment"));
        assertTrue(cdCsv.contains("export-"), "Content-Disposition must contain 'export-'");
        assertTrue(cdCsv.toLowerCase().contains(".csv"), "Filename must contain .csv extension");
    }

    @Test
    void downloadExport_unknownFormat_shouldFallbackToJson() throws Exception {
        UUID postId = UUID.randomUUID();
        BlogPost bp = new BlogPost(postId, "FALLBACK Title", "FALLBACK Body", Instant.now(), Instant.now(), user3Id);
        blogPostRepository.createBlogPost(bp);

        String url = "http://localhost:" + port + "/api/blogposts/download/" + user3Id + "?format=yml";
        ResponseEntity<byte[]> response = restTemplate.getForEntity(url, byte[].class);

        // controller's documented behavior: default to JSON when format is unknown
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
        byte[] body = response.getBody();
        assertNotNull(body);

        ExportResponse exportResp = objectMapper.readValue(body, ExportResponse.class);
        assertNotNull(exportResp);
        assertNotNull(exportResp.getBlogPosts());
        assertTrue(exportResp.getBlogPosts().size() > 0, "Fallback JSON should contain the persisted blog post");
    }

    @Test
    void downloadExport_csv_strictEscaping_shouldProduceEscapedTitleAndQuotedNewlineContent() throws Exception {
        UUID postId = UUID.randomUUID();
        String title = "CSV, Title \"with quotes\"";
        String content = "Line1\nLine2, with comma";
        BlogPost bp = new BlogPost(postId, title, content, Instant.now(), Instant.now(), user3Id);
        blogPostRepository.createBlogPost(bp);

        String url = "http://localhost:" + port + "/api/blogposts/download/" + user3Id + "?format=csv";
        ResponseEntity<byte[]> response = restTemplate.getForEntity(url, byte[].class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.valueOf("text/csv"), response.getHeaders().getContentType());

        String csv = new String(response.getBody(), StandardCharsets.UTF_8);
        assertNotNull(csv);

        // exact expected escaped title token: inner quotes doubled, whole value quoted
        String expectedEscapedTitle = "\"CSV, Title \"\"with quotes\"\"\"";
        assertTrue(csv.contains(expectedEscapedTitle), "CSV must contain the title with quotes escaped by doubling");

        // content should be quoted and still contain the newline (inside the quoted field)
        String expectedQuotedContent = "\"" + content.replace("\"", "\"\"") + "\"";
        assertTrue(csv.contains(expectedQuotedContent), "CSV must contain the content quoted (including newline and commas)");
    }

    @Test
    void downloadExport_emptyUser_shouldReturnEmptyFile() throws Exception {
        // JSON: default endpoint without ?format returns combined ExportResponse JSON
        String urlJson = "http://localhost:" + port + "/api/blogposts/download/" + user3Id;
        ResponseEntity<byte[]> respJson = restTemplate.getForEntity(urlJson, byte[].class);

        assertEquals(HttpStatus.OK, respJson.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, respJson.getHeaders().getContentType());

        // parse ExportResponse
        ExportResponse exportResp = objectMapper.readValue(respJson.getBody(), ExportResponse.class);
        assertNotNull(exportResp);
        assertNotNull(exportResp.getBlogPosts());
        assertEquals(0, exportResp.getBlogPosts().size(), "Expected empty blogPosts array for user with no blog posts");

        // CSV empty-case: still returns headers and section labels
        String urlCsv = "http://localhost:" + port + "/api/blogposts/download/" + user3Id + "?format=csv";
        ResponseEntity<byte[]> respCsv = restTemplate.getForEntity(urlCsv, byte[].class);
        assertEquals(HttpStatus.OK, respCsv.getStatusCode());
        assertEquals(MediaType.valueOf("text/csv"), respCsv.getHeaders().getContentType());
        String csv = new String(respCsv.getBody(), StandardCharsets.UTF_8);
        String[] lines = csv.split("\\r?\\n");

        // Expect at least section label and header present, but no data rows for blog posts
        assertTrue(lines.length >= 2);
        assertEquals("# BlogPosts", lines[0]);
        assertEquals("\"id\",\"title\",\"content\",\"createdAt\",\"updatedAt\",\"userId\"", lines[1]);
    }

    @Test
    void search_shouldReturnIdWhenTermMatchesTitleOnly() {
        // Arrange: Term "Java" is in the Title, but not the Content.
        BlogPost post = new BlogPost(
            UUID.randomUUID(),
            "Learn Java Programming", // Matches "Java"
            "This post is about Go basics", // Does not match "Java"
            Instant.now(),
            Instant.now(),
            user1Id
        );
        blogPostRepository.createBlogPost(post);

        String url = "http://localhost:" + port + "/api/blogposts/"+post.getId()+"/search?term=Java";

        ResponseEntity<BlogPostSearchResponse> response = restTemplate.getForEntity(
            url,
            BlogPostSearchResponse.class
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<UUID> ids = response.getBody().blogPostIds();
        assertEquals(1, ids.size(), "Should find post when term matches Title.");
        assertEquals(post.getId(), ids.get(0));
    }

    @Test
    void search_shouldReturnIdWhenTermMatchesContentOnly() {
        // Arrange: Term "Java" is in the Content, but not the Title.
        BlogPost post = new BlogPost(
            UUID.randomUUID(),
            "Python vs Go Languages", // Does not match "Java"
            "This post covers Python and Java concepts.", // Matches "Java"
            Instant.now(),
            Instant.now(),
            user1Id
        );
        blogPostRepository.createBlogPost(post);

        String url = "http://localhost:" + port + "/api/blogposts/"+post.getId()+"/search?term=Java";

        ResponseEntity<BlogPostSearchResponse> response = restTemplate.getForEntity(
            url,
            BlogPostSearchResponse.class
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<UUID> ids = response.getBody().blogPostIds();
        assertEquals(1, ids.size(), "Should find post when term matches Content.");
        assertEquals(post.getId(), ids.get(0));
    }

    @Test
    void search_shouldBeCaseSensitive() {
        BlogPost post = new BlogPost(
            UUID.randomUUID(),
            "java tutorial for beginners",
            "lower case java everywhere",
            Instant.now(),
            Instant.now(),
            user1Id
        );
        blogPostRepository.createBlogPost(post);

        String url = "http://localhost:" + port + "/api/blogposts/" + post.getId() + "/search?term=Java";

        ResponseEntity<BlogPostSearchResponse> response = restTemplate.getForEntity(
            url, BlogPostSearchResponse.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().blogPostIds().isEmpty());
    }

    @Test
    void search_shouldReturnEmptyWhenNoMatch() {
        BlogPost post = new BlogPost(
            UUID.randomUUID(),
            "Python vs Go",
            "No Java here at all",
            Instant.now(),
            Instant.now(),
            user1Id
        );
        blogPostRepository.createBlogPost(post);

        String url = "http://localhost:" + port + "/api/blogposts/" + post.getId() + "/search?term=Rust";

        ResponseEntity<BlogPostSearchResponse> response = restTemplate.getForEntity(
            url, BlogPostSearchResponse.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().blogPostIds().isEmpty());
    }

    @Test
    void search_shouldReturnMultipleIdsWhenMultiplePostsMatch() {
        BlogPost post1 = new BlogPost(UUID.randomUUID(), "Java Basics", "Learn Java", Instant.now(), Instant.now(), user1Id);
        BlogPost post2 = new BlogPost(UUID.randomUUID(), "Advanced Java", "Powerful language", Instant.now(), Instant.now(), user1Id);
        BlogPost post3 = new BlogPost(UUID.randomUUID(), "Favourite Language of All Time", "Java is Favourite language.", Instant.now(), Instant.now(), user1Id);
        BlogPost post4 = new BlogPost(UUID.randomUUID(), "Python Guide", "Python is great for scripting", Instant.now(), Instant.now(), user1Id);

        blogPostRepository.createBlogPost(post1);
        blogPostRepository.createBlogPost(post2);
        blogPostRepository.createBlogPost(post3);
        blogPostRepository.createBlogPost(post4);

        String url = "http://localhost:" + port + "/api/blogposts/" + post1.getId() + "/search?term=Java";

        ResponseEntity<BlogPostSearchResponse> response = restTemplate.getForEntity(
            url, BlogPostSearchResponse.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<UUID> ids = response.getBody().blogPostIds();
        assertEquals(3, ids.size());
        assertTrue(ids.contains(post1.getId()));
        assertTrue(ids.contains(post2.getId()));
    }

    @Test
    void search_shouldReturnEmptyListWhenTermIsEmpty() {
        // Arrange: Create one post to ensure the database isn't empty, but the search term is.
        BlogPost post = new BlogPost(
            UUID.randomUUID(),
            "A title",
            "Some content",
            Instant.now(),
            Instant.now(),
            user1Id
        );
        blogPostRepository.createBlogPost(post);

        // Act: Search with an empty term
        String url = "http://localhost:" + port + "/api/blogposts/" + post.getId() + "/search?term=";

        ResponseEntity<BlogPostSearchResponse> response = restTemplate.getForEntity(
            url, BlogPostSearchResponse.class
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        // Verify that the list of results is empty
        assertTrue(response.getBody().blogPostIds().isEmpty(), "Expected an empty list when the search term is empty.");
    }

    @Test
    void search_shouldReturnEmptyListWhenTermIsWhitespace() {
        // Arrange: Create one post to ensure the database isn't empty, but the search term is whitespace.
        BlogPost post = new BlogPost(
            UUID.randomUUID(),
            "A title",
            "Some content",
            Instant.now(),
            Instant.now(),
            user1Id
        );
        blogPostRepository.createBlogPost(post);

        // Act: Search with a whitespace term (the framework handles URL encoding spaces)
        String url = "http://localhost:" + port + "/api/blogposts/" + post.getId() + "/search?term=   ";

        ResponseEntity<BlogPostSearchResponse> response = restTemplate.getForEntity(
            url, BlogPostSearchResponse.class
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        // Verify that the list of results is empty
        assertTrue(response.getBody().blogPostIds().isEmpty(), "Expected an empty list when the search term is whitespace only.");
    }

    @Test
    void search_shouldReturnEmptyListWhenTermIsOmitted() {
        // Arrange: Create one post to ensure the database isn't empty.
        BlogPost post = new BlogPost(
            UUID.randomUUID(),
            "A title",
            "Some content",
            Instant.now(),
            Instant.now(),
            user1Id
        );
        blogPostRepository.createBlogPost(post);

        // Act: Search, but omit the '?term=' query parameter.
        // This causes the @RequestParam String term to be bound as null.
        String url = "http://localhost:" + port + "/api/blogposts/" + post.getId() + "/search";

        ResponseEntity<BlogPostSearchResponse> response = restTemplate.getForEntity(
            url, BlogPostSearchResponse.class
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().blogPostIds().isEmpty(), "Expected an empty list when the search term is null (omitted).");
    }

    @Test
    void softDelete_shouldMarkPostAsSoftDeleted() {
        // Arrange: create a post
        BlogPost post = new BlogPost(UUID.randomUUID(), "T1", "C1",
                Instant.now(), Instant.now(), user1Id);
        blogPostRepository.createBlogPost(post);

        // Act: soft delete
        ResponseEntity<Void> response = restTemplate.exchange(
                "http://localhost:" + port + "/api/blogposts/" + post.getId() + "/soft-delete",
                HttpMethod.PATCH,
                null,
                Void.class
        );

        // Assert API result
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

        // Assert DB state
        BlogPost updated = blogPostRepository.getBlogPost(post.getId(), true).orElseThrow();
        assertTrue(updated.isSoftDeleted());
        assertNotNull(updated.getDeletedAt());
    }


    @Test
    void getBlogPost_defaultFlag_shouldNotReturnSoftDeletedPost() {
        // Arrange: create & soft delete
        BlogPost post = new BlogPost(UUID.randomUUID(), "T1", "C1",
                Instant.now(), Instant.now(), user1Id);
        blogPostRepository.createBlogPost(post);
        blogPostRepository.softDelete(post.getId());

        // Act
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/blogposts/" + post.getId(),
                String.class
        );

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void getBlogPost_withIncludeSoftDeleted_shouldReturnSoftDeletedPost() {
        // Arrange
        BlogPost post = new BlogPost(UUID.randomUUID(), "T1", "C1",
                Instant.now(), Instant.now(), user1Id);
        blogPostRepository.createBlogPost(post);
        blogPostRepository.softDelete(post.getId());

        // Act
        ResponseEntity<BlogPost> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/blogposts/" + post.getId() + "?includeSoftDeleted=true",
                BlogPost.class
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSoftDeleted());
    }

    @Test
    void getPaginated_shouldRespectIncludeSoftDeletedFlag() {
        // Arrange
        BlogPost p1 = new BlogPost(UUID.randomUUID(), "A", "A1",
                Instant.now(), Instant.now(), user1Id);
        BlogPost p2 = new BlogPost(UUID.randomUUID(), "B", "B1",
                Instant.now(), Instant.now(), user1Id);

        blogPostRepository.createBlogPost(p1);
        blogPostRepository.createBlogPost(p2);

        blogPostRepository.softDelete(p2.getId());

        // Case 1: default (exclude soft-deleted)
        ResponseEntity<Page<BlogPost>> resp1 = restTemplate.exchange(
                "http://localhost:" + port + "/api/blogposts",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );

        assertEquals(1, resp1.getBody().getContent().size());
        assertEquals(p1.getId(), resp1.getBody().getContent().get(0).getId());

        // Case 2: include soft-deleted
        ResponseEntity<Page<BlogPost>> resp2 = restTemplate.exchange(
                "http://localhost:" + port + "/api/blogposts?includeSoftDeleted=true",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );

        assertEquals(2, resp2.getBody().getContent().size());
    }
    @Test
    void submitForReview_shouldTransitionDraftToPendingReview() {
        BlogPost draftPost = new BlogPost(UUID.randomUUID(), "Draft Title", "Draft Content", Instant.now(), Instant.now(), user1Id);
        draftPost.setState(BlogPost.State.DRAFT);
        blogPostRepository.createBlogPost(draftPost);

        ResponseEntity<BlogPost> response = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/blogposts/" + draftPost.getId() + "/submit-for-review",
            null,
            BlogPost.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(BlogPost.State.PENDING_REVIEW, response.getBody().getState());
    }

    @Test
    void approveBlogPost_shouldTransitionPendingReviewToApproved() {
        BlogPost pendingPost = new BlogPost(UUID.randomUUID(), "Pending Title", "Pending Content", Instant.now(), Instant.now(), user1Id);
        pendingPost.setState(BlogPost.State.PENDING_REVIEW);
        blogPostRepository.createBlogPost(pendingPost);

        ResponseEntity<BlogPost> response = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/blogposts/" + pendingPost.getId() + "/approve",
            null,
            BlogPost.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(BlogPost.State.APPROVED, response.getBody().getState());
    }

    @Test
    void rejectBlogPost_shouldTransitionPendingReviewToRejected() {
        BlogPost pendingPost = new BlogPost(UUID.randomUUID(), "Pending Title", "Pending Content", Instant.now(), Instant.now(), user1Id);
        pendingPost.setState(BlogPost.State.PENDING_REVIEW);
        blogPostRepository.createBlogPost(pendingPost);

        ResponseEntity<BlogPost> response = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/blogposts/" + pendingPost.getId() + "/reject",
            null,
            BlogPost.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(BlogPost.State.REJECTED, response.getBody().getState());
    }

    @Test
    void submitForReview_shouldFailIfNotDraft() {
        BlogPost approvedPost = new BlogPost(UUID.randomUUID(), "Approved Title", "Approved Content", Instant.now(), Instant.now(), user1Id);
        approvedPost.setState(BlogPost.State.APPROVED);
        blogPostRepository.createBlogPost(approvedPost);

        ResponseEntity<String> response = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/blogposts/" + approvedPost.getId() + "/submit-for-review",
            null,
            String.class
        );

        assertTrue(response.getStatusCode() == HttpStatus.BAD_REQUEST || response.getStatusCode() == HttpStatus.CONFLICT);
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("Only DRAFT posts can be submitted for review."));
    }

    @Test
    void approveBlogPost_shouldFailIfNotPendingReview() {
        BlogPost draftPost = new BlogPost(UUID.randomUUID(), "Draft Title", "Draft Content", Instant.now(), Instant.now(), user1Id);
        draftPost.setState(BlogPost.State.DRAFT);
        blogPostRepository.createBlogPost(draftPost);

        ResponseEntity<String> response = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/blogposts/" + draftPost.getId() + "/approve",
            null,
            String.class
        );

        assertTrue(response.getStatusCode() == HttpStatus.BAD_REQUEST || response.getStatusCode() == HttpStatus.CONFLICT);
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("Only PENDING_REVIEW posts can be approved."));
    }

    @Test
    void rejectBlogPost_shouldFailIfNotPendingReview() {
        BlogPost draftPost = new BlogPost(UUID.randomUUID(), "Draft Title", "Draft Content", Instant.now(), Instant.now(), user1Id);
        draftPost.setState(BlogPost.State.DRAFT);
        blogPostRepository.createBlogPost(draftPost);

        ResponseEntity<String> response = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/blogposts/" + draftPost.getId() + "/reject",
            null,
            String.class
        );

        assertTrue(response.getStatusCode() == HttpStatus.BAD_REQUEST || response.getStatusCode() == HttpStatus.CONFLICT);
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("Only PENDING_REVIEW posts can be rejected."));
    }

}
