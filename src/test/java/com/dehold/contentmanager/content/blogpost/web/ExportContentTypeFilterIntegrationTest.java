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

public class ExportContentTypeFilterIntegrationTest extends ContentManagerApplicationTests {

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

    private UUID uid;

    @BeforeEach
    void setup() {
        CreateUserRequest req = new CreateUserRequest();
        req.setAlias("filter-user");
        req.setEmail("filter-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com");
        req.setUsername("TestUser" + UUID.randomUUID());
        req.setPassword("TestPassword" + UUID.randomUUID());
        User u = userService.createUser(req);
        uid = u.getId();
    }

    @Test
    void export_contentType_blogpost_jsonOnlyContainsBlogPosts() throws Exception {
        BlogPost bp = new BlogPost(UUID.randomUUID(), "BP1", "body", Instant.now(), Instant.now(), uid);
        blogPostRepository.createBlogPost(bp);
        // other types present but should not be exported when filtering
        SupportRequest sr = new SupportRequest(UUID.randomUUID(), uid, "req", UUID.randomUUID(), UUID.randomUUID(), Instant.now(), Instant.now());
        supportRequestRepository.create(sr);
        SupportResponse sresp = new SupportResponse(UUID.randomUUID(), uid, "resp", UUID.randomUUID(), Instant.now(), Instant.now());
        supportResponseRepository.create(sresp);

        String url = "http://localhost:" + port + "/api/blogposts/download/" + uid + "?format=json&contentType=blogpost";
        ResponseEntity<byte[]> resp = restTemplate.getForEntity(url, byte[].class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, resp.getHeaders().getContentType());

        ExportResponse parsed = objectMapper.readValue(resp.getBody(), ExportResponse.class);
        assertNotNull(parsed);
        assertTrue(parsed.getBlogPosts().size() >= 1);
        assertTrue(parsed.getSupportRequests().isEmpty(), "SupportRequests should be empty when filtering by blogpost");
        assertTrue(parsed.getSupportResponses().isEmpty(), "SupportResponses should be empty when filtering by blogpost");
    }

    @Test
    void export_contentType_supportrequest_csvOnlyContainsSupportRequestsSection() throws Exception {
        // create all types
        BlogPost bp = new BlogPost(UUID.randomUUID(), "BP2", "body", Instant.now(), Instant.now(), uid);
        blogPostRepository.createBlogPost(bp);
        SupportRequest sr = new SupportRequest(UUID.randomUUID(), uid, "req2", UUID.randomUUID(), UUID.randomUUID(), Instant.now(), Instant.now());
        supportRequestRepository.create(sr);
        SupportResponse sresp = new SupportResponse(UUID.randomUUID(), uid, "resp2", UUID.randomUUID(), Instant.now(), Instant.now());
        supportResponseRepository.create(sresp);

        String url = "http://localhost:" + port + "/api/blogposts/download/" + uid + "?format=csv&contentType=supportrequest";
        ResponseEntity<byte[]> resp = restTemplate.getForEntity(url, byte[].class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(MediaType.valueOf("text/csv"), resp.getHeaders().getContentType());

        String csv = new String(resp.getBody(), StandardCharsets.UTF_8);
        // BlogPosts section might still be present depending on conversion logic; our converter produces sections for each collection
        // For the filtered case, BlogPosts section should contain header but no data rows
        assertTrue(csv.contains("# BlogPosts"), "CSV should contain BlogPosts section label");
        // Ensure supportRequests section contains the text we inserted
        assertTrue(csv.contains("# SupportRequests"));
        assertTrue(csv.contains("\"req2\"") || csv.contains("req2"));
        // Ensure supportResponses section is empty (no data rows), but header exists
        assertTrue(csv.contains("# SupportResponses"));
        // Since filter is supportrequest, SupportResponses should be empty
        // naive check: ensure our support response text isn't present
        assertFalse(csv.contains("resp2"));
    }

    @Test
    void export_contentType_supportresponse_xmlOnlyContainsSupportResponses() throws Exception {
        BlogPost bp = new BlogPost(UUID.randomUUID(), "BP3", "body", Instant.now(), Instant.now(), uid);
        blogPostRepository.createBlogPost(bp);

        SupportRequest sr = new SupportRequest(UUID.randomUUID(), uid, "req3", UUID.randomUUID(), UUID.randomUUID(), Instant.now(), Instant.now());
        supportRequestRepository.create(sr);
        SupportResponse sresp = new SupportResponse(UUID.randomUUID(), uid, "resp3", UUID.randomUUID(), Instant.now(), Instant.now());
        supportResponseRepository.create(sresp);

        String url = "http://localhost:" + port + "/api/blogposts/download/" + uid + "?format=xml&contentType=supportresponse";
        ResponseEntity<byte[]> resp = restTemplate.getForEntity(url, byte[].class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(MediaType.APPLICATION_XML, resp.getHeaders().getContentType());

        String xml = new String(resp.getBody(), StandardCharsets.UTF_8);
        assertTrue(xml.contains("<supportResponses>"));
        assertTrue(xml.contains("<supportResponse>"));
        assertTrue(xml.contains("<text>resp3</text>"));

        // ensure blogPosts and supportRequests sections exist but contain no data
        assertTrue(xml.contains("<blogPosts>"));
        assertTrue(xml.contains("<supportRequests>"));
        // ensure our blogpost title is NOT present
        assertFalse(xml.contains("<title>BP3</title>"));
    }

    @Test
    void export_contentType_caseInsensitive_and_invalidTypeHandling() throws Exception {
        BlogPost bp = new BlogPost(UUID.randomUUID(), "BP4", "body", Instant.now(), Instant.now(), uid);
        blogPostRepository.createBlogPost(bp);

        // case-insensitive
        String urlUpper = "http://localhost:" + port + "/api/blogposts/download/" + uid + "?format=json&contentType=BlogPost";
        ResponseEntity<byte[]> respUpper = restTemplate.getForEntity(urlUpper, byte[].class);
        assertEquals(HttpStatus.OK, respUpper.getStatusCode());
        ExportResponse parsedUpper = objectMapper.readValue(respUpper.getBody(), ExportResponse.class);
        assertTrue(parsedUpper.getBlogPosts().size() > 0);

        // invalid contentType -> 400 Bad Request
        String urlBad = "http://localhost:" + port + "/api/blogposts/download/" + uid + "?format=json&contentType=notatype";
        ResponseEntity<byte[]> respBad = restTemplate.getForEntity(urlBad, byte[].class);
        assertEquals(HttpStatus.BAD_REQUEST, respBad.getStatusCode());
        String body = new String(respBad.getBody(), StandardCharsets.UTF_8);
        assertTrue(body.toLowerCase().contains("invalid contenttype"));
    }
}
