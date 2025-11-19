package com.dehold.contentmanager.content.blogpost.web;


import com.dehold.contentmanager.ContentManagerApplicationTests;
import com.dehold.contentmanager.content.blogpost.model.BlogPost;
import com.dehold.contentmanager.content.blogpost.repository.BlogPostRepository;
import com.dehold.contentmanager.content.customersupport.model.SupportRequest;
import com.dehold.contentmanager.content.customersupport.model.SupportResponse;
import com.dehold.contentmanager.content.customersupport.repository.SupportRequestRepository;
import com.dehold.contentmanager.content.customersupport.repository.SupportResponseRepository;
import com.dehold.contentmanager.user.service.UserService;
import com.dehold.contentmanager.user.web.dto.CreateUserRequest;
import com.dehold.contentmanager.user.model.User;
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


public class ExportXmlIntegrationTest extends ContentManagerApplicationTests {

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

    @BeforeEach
    void setup() {
        // create a fresh user for each test
        CreateUserRequest req = new CreateUserRequest();
        req.setAlias("xml-export-user");
        req.setEmail("xml-export+" + UUID.randomUUID().toString().substring(0, 8) + "@example.com");
        req.setUsername("TestUser" + UUID.randomUUID());
        req.setPassword("TestPassword" + UUID.randomUUID());
        User u = userService.createUser(req);
        assertNotNull(u);
    }

    @Test
    void xmlExport_shouldReturnApplicationXmlAndAttachmentFilename() throws Exception {
        // create a user and a blogpost
        CreateUserRequest req = new CreateUserRequest();
        req.setAlias("xmluser2");
        req.setEmail("xmluser2+" + UUID.randomUUID().toString().substring(0, 8) + "@example.com");
        req.setUsername("TestUser" + UUID.randomUUID());
        req.setPassword("TestPassword" + UUID.randomUUID());
        User u = userService.createUser(req);
        UUID uid = u.getId();

        BlogPost bp = new BlogPost(UUID.randomUUID(), "XML Title", "XML Body", Instant.now(), Instant.now(), uid);
        blogPostRepository.createBlogPost(bp);

        String url = "http://localhost:" + port + "/api/blogposts/download/" + uid + "?format=xml";
        ResponseEntity<byte[]> resp = restTemplate.getForEntity(url, byte[].class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(MediaType.APPLICATION_XML, resp.getHeaders().getContentType());
        String cd = resp.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        assertNotNull(cd);
        assertTrue(cd.toLowerCase().contains("attachment"));
        assertTrue(cd.contains("export-"));
        assertTrue(cd.toLowerCase().contains(".xml"));

        assertNotNull(resp.getBody());
        String xml = new String(resp.getBody(), StandardCharsets.UTF_8);
        assertTrue(xml.startsWith("<?xml"));
        assertTrue(xml.contains("<blogPosts>"));
        assertTrue(xml.contains("<blogPost>"));
        assertTrue(xml.contains("<title>XML Title</title>"));
        assertTrue(xml.contains("<userId>" + uid.toString() + "</userId>"));
    }

    @Test
    void xmlExport_caseInsensitiveFormatParam_shouldReturnXml() throws Exception {
        CreateUserRequest req = new CreateUserRequest();
        req.setAlias("xmluser3");
        req.setEmail("xmluser3+" + UUID.randomUUID().toString().substring(0, 8) + "@example.com");
        req.setUsername("TestUser" + UUID.randomUUID());
        req.setPassword("TestPassword" + UUID.randomUUID());
        User u = userService.createUser(req);
        UUID uid = u.getId();

        BlogPost bp = new BlogPost(UUID.randomUUID(), "Case XML", "Body", Instant.now(), Instant.now(), uid);
        blogPostRepository.createBlogPost(bp);

        String url = "http://localhost:" + port + "/api/blogposts/download/" + uid + "?format=XML";
        ResponseEntity<byte[]> resp = restTemplate.getForEntity(url, byte[].class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(MediaType.APPLICATION_XML, resp.getHeaders().getContentType());
    }

    @Test
    void xmlExport_shouldIncludeSupportRequestsAndResponses() throws Exception {
        CreateUserRequest req = new CreateUserRequest();
        req.setAlias("xmluser4");
        req.setEmail("xmluser4+" + UUID.randomUUID().toString().substring(0, 8) + "@example.com");
        req.setUsername("TestUser" + UUID.randomUUID());
        req.setPassword("TestPassword" + UUID.randomUUID());
        User u = userService.createUser(req);
        UUID uid = u.getId();


        BlogPost bp = new BlogPost(UUID.randomUUID(), "B", "B", Instant.now(), Instant.now(), uid);
        blogPostRepository.createBlogPost(bp);

        SupportRequest sr = new SupportRequest(UUID.randomUUID(), uid, "Need XML", UUID.randomUUID(), UUID.randomUUID(), Instant.now(), Instant.now());
        supportRequestRepository.create(sr);

        SupportResponse sresp = new SupportResponse(UUID.randomUUID(), uid, "XML Reply", UUID.randomUUID(), Instant.now(), Instant.now());
        supportResponseRepository.create(sresp);

        String url = "http://localhost:" + port + "/api/blogposts/download/" + uid + "?format=xml";
        ResponseEntity<byte[]> resp = restTemplate.getForEntity(url, byte[].class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        String xml = new String(resp.getBody(), StandardCharsets.UTF_8);
        assertTrue(xml.contains("<supportRequests>"));
        assertTrue(xml.contains("<supportRequest>"));
        assertTrue(xml.contains("<text>Need XML</text>"));
        assertTrue(xml.contains("<supportResponses>"));
        assertTrue(xml.contains("<supportResponse>"));
        assertTrue(xml.contains("<text>XML Reply</text>"));
    }

    @Test
    void xmlExport_unknownFormat_shouldFallbackToJson() throws Exception {
        CreateUserRequest req = new CreateUserRequest();
        req.setAlias("xmluser5");
        req.setEmail("xmluser5+" + UUID.randomUUID().toString().substring(0, 8) + "@example.com");
        req.setUsername("TestUser" + UUID.randomUUID());
        req.setPassword("TestPassword" + UUID.randomUUID());
        User u = userService.createUser(req);
        UUID uid = u.getId();

        BlogPost bp = new BlogPost(UUID.randomUUID(), "Fallback", "Body", Instant.now(), Instant.now(), uid);
        blogPostRepository.createBlogPost(bp);

        String url = "http://localhost:" + port + "/api/blogposts/download/" + uid + "?format=bad";
        ResponseEntity<byte[]> resp = restTemplate.getForEntity(url, byte[].class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        // fallback to JSON
        assertEquals(MediaType.APPLICATION_JSON, resp.getHeaders().getContentType());
    }

    @Test
    void xmlExport_emptyUser_shouldReturnXmlWithEmptyCollections() throws Exception {
        CreateUserRequest req = new CreateUserRequest();
        req.setAlias("xmlempty");
        req.setEmail("xmlempty+" + UUID.randomUUID().toString().substring(0, 8) + "@example.com");
        req.setUsername("TestUser" + UUID.randomUUID());
        req.setPassword("TestPassword" + UUID.randomUUID());
        User empty = userService.createUser(req);
        UUID uid = empty.getId();

        String url = "http://localhost:" + port + "/api/blogposts/download/" + uid + "?format=xml";
        ResponseEntity<byte[]> resp = restTemplate.getForEntity(url, byte[].class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(MediaType.APPLICATION_XML, resp.getHeaders().getContentType());
        String xml = new String(resp.getBody(), StandardCharsets.UTF_8);
        assertTrue(xml.contains("<blogPosts>"));
        assertTrue(xml.contains("<supportRequests>"));
        assertTrue(xml.contains("<supportResponses>"));
        // empty collections should not include blogPost/supportRequest/supportResponse tags with data
        // basic check: ensure content does not contain non-empty id elements
        assertFalse(xml.matches("(?s).*<id>\\s*[^\\s<].*"), "XML should not contain data id elements for empty user");
    }
}
