package com.dehold.contentmanager.content.blogpost.web;


import com.dehold.contentmanager.ContentManagerApplicationTests;
import com.dehold.contentmanager.content.blogpost.export.ExportResponse;
import com.dehold.contentmanager.content.blogpost.model.BlogPost;
import com.dehold.contentmanager.content.blogpost.repository.BlogPostRepository;
import com.dehold.contentmanager.content.customersupport.model.SupportRequest;
import com.dehold.contentmanager.content.customersupport.model.SupportResponse;
import com.dehold.contentmanager.content.customersupport.repository.SupportRequestRepository;
import com.dehold.contentmanager.content.customersupport.repository.SupportResponseRepository;
import com.dehold.contentmanager.user.service.UserService;
import com.dehold.contentmanager.user.web.dto.CreateUserRequest;
import com.dehold.contentmanager.user.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;


public class BlogPostCombinedExportIntegrationTest extends ContentManagerApplicationTests {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private BlogPostRepository blogPostRepository;

    @Autowired
    private SupportRequestRepository supportRequestRepository;

    @Autowired
    private SupportResponseRepository supportResponseRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID userId;

    @BeforeEach
    void setup() {
        CreateUserRequest req = new CreateUserRequest();
        req.setAlias("combined-export-user");
        req.setEmail("combined-export+" + UUID.randomUUID().toString().substring(0,8) + "@example.com");
        req.setUsername("TestUser"+ UUID.randomUUID());
        req.setPassword("TestPassword"+ UUID.randomUUID());
        User u = userService.createUser(req);
        assertNotNull(u);
        userId = u.getId();
    }

    @Test
    void jsonExport_shouldReturnAllThreeCollections_andContainPersistedData() throws Exception {
        // arrange: persist one of each
        BlogPost bp = new BlogPost(UUID.randomUUID(), "BTitle", "BBody", Instant.now(), Instant.now(), userId);
        blogPostRepository.createBlogPost(bp);

        SupportRequest sr = new SupportRequest( UUID.randomUUID(), userId, "Need help",  UUID.randomUUID(), UUID.randomUUID(), Instant.now(), Instant.now());
        supportRequestRepository.create(sr);
        SupportResponse sresp = new SupportResponse( UUID.randomUUID(), userId, "Reply text",  UUID.randomUUID(), Instant.now(), Instant.now());
        supportResponseRepository.create(sresp);

        // act
        String url = "http://localhost:" + port + "/api/blogposts/download/" + userId + "?format=json";
        ResponseEntity<byte[]> resp = restTemplate.getForEntity(url, byte[].class);

        // assert headers
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, resp.getHeaders().getContentType());
        String cd = resp.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        assertNotNull(cd);
        assertTrue(cd.toLowerCase().contains("attachment"));
        assertTrue(cd.contains("export-"));
        assertTrue(cd.toLowerCase().contains(".json"));

        // assert payload
        ExportResponse parsed = objectMapper.readValue(resp.getBody(), ExportResponse.class);
        assertNotNull(parsed);
        assertTrue(parsed.getBlogPosts().stream().anyMatch(b -> "BTitle".equals(b.getTitle())));
        assertTrue(parsed.getSupportRequests().stream().anyMatch(r -> "Need help".equals(r.getText())));
        assertTrue(parsed.getSupportResponses().stream().anyMatch(r -> "Reply text".equals(r.getText())));
    }

    @Test
    void csvExport_shouldContainThreeSections_andIncludePersistedValues() throws Exception {
        // arrange
        BlogPost bp = new BlogPost(UUID.randomUUID(), "CSV Title", "CSV Body", Instant.now(), Instant.now(), userId);
        blogPostRepository.createBlogPost(bp);

        SupportRequest sr = new SupportRequest(UUID.randomUUID(), userId, "SR Text",  UUID.randomUUID(), UUID.randomUUID(), Instant.now(), Instant.now());
        supportRequestRepository.create(sr);

        SupportResponse sresp = new SupportResponse(UUID.randomUUID(), userId, "SR Reply",  UUID.randomUUID(), Instant.now(), Instant.now());
        supportResponseRepository.create(sresp);

        // act
        String url = "http://localhost:" + port + "/api/blogposts/download/" + userId + "?format=csv";
        ResponseEntity<byte[]> resp = restTemplate.getForEntity(url, byte[].class);

        // assert headers
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(MediaType.valueOf("text/csv"), resp.getHeaders().getContentType());
        String cd = resp.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        assertNotNull(cd);
        assertTrue(cd.toLowerCase().contains("attachment"));
        assertTrue(cd.contains("export-"));
        assertTrue(cd.toLowerCase().contains(".csv"));

        // assert CSV content (sections + simple data checks)
        String csv = new String(resp.getBody(), StandardCharsets.UTF_8);
        assertTrue(csv.contains("# BlogPosts"), "CSV must contain BlogPosts section");
        assertTrue(csv.contains("\"id\",\"title\",\"content\",\"createdAt\",\"updatedAt\",\"userId\""));
        assertTrue(csv.contains("# SupportRequests"), "CSV must contain SupportRequests section");
        assertTrue(csv.contains("\"id\",\"userId\",\"text\",\"customerId\",\"createdAt\",\"updatedAt\""));
        assertTrue(csv.contains("# SupportResponses"), "CSV must contain SupportResponses section");
        assertTrue(csv.contains("\"id\",\"supportRequestId\",\"userId\",\"text\",\"createdAt\",\"updatedAt\""));

        // data presence
        assertTrue(csv.contains("\"CSV Title\""), "CSV should include blog post title");
        assertTrue(csv.contains("\"SR Text\""), "CSV should include support request text");
        assertTrue(csv.contains("\"SR Reply\""), "CSV should include support response text");
        // userId appears somewhere
        assertTrue(csv.contains(userId.toString()));
    }

    @Test
    void defaultFormat_noParam_shouldReturnJsonExport() throws Exception {
        BlogPost bp = new BlogPost(UUID.randomUUID(), "Default Title", "Default Body", Instant.now(), Instant.now(), userId);
        blogPostRepository.createBlogPost(bp);

        String url = "http://localhost:" + port + "/api/blogposts/download/" + userId;
        ResponseEntity<byte[]> resp = restTemplate.getForEntity(url, byte[].class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, resp.getHeaders().getContentType());

        ExportResponse parsed = objectMapper.readValue(resp.getBody(), ExportResponse.class);
        assertNotNull(parsed);
        assertTrue(parsed.getBlogPosts().stream().anyMatch(b -> "Default Title".equals(b.getTitle())));
    }

    @Test
    void formatParam_isCaseInsensitive() throws Exception {
        BlogPost bp = new BlogPost(UUID.randomUUID(), "CaseTitle", "CaseBody", Instant.now(), Instant.now(), userId);
        blogPostRepository.createBlogPost(bp);

        String url = "http://localhost:" + port + "/api/blogposts/download/" + userId + "?format=CSV";
        ResponseEntity<byte[]> resp = restTemplate.getForEntity(url, byte[].class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(MediaType.valueOf("text/csv"), resp.getHeaders().getContentType());
    }

    @Test
    void unknownFormat_shouldFallbackToJson_defaultBehavior() throws Exception {
        BlogPost bp = new BlogPost(UUID.randomUUID(), "Fallback", "Body", Instant.now(), Instant.now(), userId);
        blogPostRepository.createBlogPost(bp);

        String url = "http://localhost:" + port + "/api/blogposts/download/" + userId + "?format=xml";
        ResponseEntity<byte[]> resp = restTemplate.getForEntity(url, byte[].class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, resp.getHeaders().getContentType());

        ExportResponse parsed = objectMapper.readValue(resp.getBody(), ExportResponse.class);
        assertNotNull(parsed);
        assertTrue(parsed.getBlogPosts().stream().anyMatch(b -> "Fallback".equals(b.getTitle())));
    }


    @Test
    void emptyUser_shouldReturnEmptyCollections_inJson_andHeadersOnlyInCsv() throws Exception {
        // create a new user with no content
        CreateUserRequest req = new CreateUserRequest();
        req.setAlias("empty-user");
        req.setEmail("empty-user+" + UUID.randomUUID().toString().substring(0,8) + "@example.com");
        req.setUsername("TestUser"+ UUID.randomUUID());
        req.setPassword("TestPassword"+ UUID.randomUUID());
        User empty = userService.createUser(req);

        // JSON check
        String urlJson = "http://localhost:" + port + "/api/blogposts/download/" + empty.getId() + "?format=json";
        ResponseEntity<byte[]> rjson = restTemplate.getForEntity(urlJson, byte[].class);
        assertEquals(HttpStatus.OK, rjson.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, rjson.getHeaders().getContentType());
        ExportResponse parsed = objectMapper.readValue(rjson.getBody(), ExportResponse.class);
        assertNotNull(parsed);
        assertTrue(parsed.getBlogPosts().isEmpty());
        assertTrue(parsed.getSupportRequests().isEmpty());
        assertTrue(parsed.getSupportResponses().isEmpty());

        // CSV check: header lines are present, no data rows for blog posts
        String urlCsv = "http://localhost:" + port + "/api/blogposts/download/" + empty.getId() + "?format=csv";
        ResponseEntity<byte[]> rcsv = restTemplate.getForEntity(urlCsv, byte[].class);
        assertEquals(HttpStatus.OK, rcsv.getStatusCode());
        assertEquals(MediaType.valueOf("text/csv"), rcsv.getHeaders().getContentType());
        String csv = new String(rcsv.getBody(), StandardCharsets.UTF_8);
        String[] lines = csv.split("\\r?\\n");
        assertTrue(lines.length >= 2);
        assertEquals("# BlogPosts", lines[0]);
        assertEquals("\"id\",\"title\",\"content\",\"createdAt\",\"updatedAt\",\"userId\"", lines[1]);
    }

    @Test
    void contentLengthHeader_shouldMatchBodyLength_forJsonAndCsv() throws Exception {
        BlogPost bp = new BlogPost(UUID.randomUUID(), "LenTitle", "LenBody", Instant.now(), Instant.now(), userId);
        blogPostRepository.createBlogPost(bp);

        // JSON
        String urlJson = "http://localhost:" + port + "/api/blogposts/download/" + userId + "?format=json";
        ResponseEntity<byte[]> rjson = restTemplate.getForEntity(urlJson, byte[].class);
        assertEquals(HttpStatus.OK, rjson.getStatusCode());
        long lenHeader = rjson.getHeaders().getContentLength();
        byte[] bodyJson = rjson.getBody();
        assertNotNull(bodyJson);
        assertEquals(bodyJson.length, lenHeader);

        // CSV
        String urlCsv = "http://localhost:" + port + "/api/blogposts/download/" + userId + "?format=csv";
        ResponseEntity<byte[]> rcsv = restTemplate.getForEntity(urlCsv, byte[].class);
        assertEquals(HttpStatus.OK, rcsv.getStatusCode());
        long lenHeaderCsv = rcsv.getHeaders().getContentLength();
        byte[] bodyCsv = rcsv.getBody();
        assertNotNull(bodyCsv);
        assertEquals(bodyCsv.length, lenHeaderCsv);
    }
}