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
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;


public class ExportEndpointsAllCasesIntegrationTest extends ContentManagerApplicationTests {

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
        req.setAlias("export-test-user");
        req.setEmail("export-test+" + UUID.randomUUID().toString().substring(0, 8) + "@example.com");
        req.setUsername("TestUser" + UUID.randomUUID());
        req.setPassword("TestPassword" + UUID.randomUUID());
        User u = userService.createUser(req);
        userId = u.getId();
    }

    @Test
    void singleUser_get_defaultJson_returnsExportResponse_andFilenamePreserved() throws Exception {
        BlogPost bp = new BlogPost(UUID.randomUUID(), "S1", "body", Instant.now(), Instant.now(), userId);
        blogPostRepository.createBlogPost(bp);

        String url = "http://localhost:" + port + "/api/blogposts/download/" + userId;
        ResponseEntity<byte[]> resp = restTemplate.getForEntity(url, byte[].class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, resp.getHeaders().getContentType());

        String cd = resp.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        assertNotNull(cd);
        assertTrue(cd.contains("export-" + userId.toString()));
        assertTrue(cd.toLowerCase().contains(".json"));

        ExportResponse parsed = objectMapper.readValue(resp.getBody(), ExportResponse.class);
        assertNotNull(parsed);
        assertTrue(parsed.getBlogPosts().stream().anyMatch(p -> p.getTitle().equals("S1")));
    }

    @Test
    void singleUser_get_csv_and_xml_formats_and_headers() throws Exception {
        BlogPost bp = new BlogPost(UUID.randomUUID(), "S2", "body", Instant.now(), Instant.now(), userId);
        blogPostRepository.createBlogPost(bp);

        // CSV
        String csvUrl = "http://localhost:" + port + "/api/blogposts/download/" + userId + "?format=csv";
        ResponseEntity<byte[]> csvResp = restTemplate.getForEntity(csvUrl, byte[].class);
        assertEquals(HttpStatus.OK, csvResp.getStatusCode());
        assertEquals(MediaType.valueOf("text/csv"), csvResp.getHeaders().getContentType());
        String csvCd = csvResp.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        assertNotNull(csvCd);
        assertTrue(csvCd.contains("export-" + userId.toString()));
        assertTrue(csvCd.toLowerCase().contains(".csv"));
        String csvBody = new String(csvResp.getBody(), StandardCharsets.UTF_8);
        assertTrue(csvBody.contains("# BlogPosts"));
        assertTrue(csvBody.contains("\"S2\"") || csvBody.contains("S2"));

        // XML
        String xmlUrl = "http://localhost:" + port + "/api/blogposts/download/" + userId + "?format=xml";
        ResponseEntity<byte[]> xmlResp = restTemplate.getForEntity(xmlUrl, byte[].class);
        assertEquals(HttpStatus.OK, xmlResp.getStatusCode());
        assertEquals(MediaType.APPLICATION_XML, xmlResp.getHeaders().getContentType());
        String xmlCd = xmlResp.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        assertNotNull(xmlCd);
        assertTrue(xmlCd.contains("export-" + userId.toString()));
        assertTrue(xmlCd.toLowerCase().contains(".xml"));
        String xmlBody = new String(xmlResp.getBody(), StandardCharsets.UTF_8);
        assertTrue(xmlBody.contains("<blogPosts>"));
        assertTrue(xmlBody.contains("<title>S2</title>"));
    }

    @Test
    void singleUser_filterContentType_blogpost_returns_only_blogposts() throws Exception {
        // create all types
        BlogPost bp = new BlogPost(UUID.randomUUID(), "CF1", "b", Instant.now(), Instant.now(), userId);
        blogPostRepository.createBlogPost(bp);
        SupportRequest sr = new SupportRequest(UUID.randomUUID(), userId, "SR-CF", null, UUID.randomUUID(), Instant.now(), Instant.now());
        supportRequestRepository.create(sr);
        SupportResponse srsp = new SupportResponse(UUID.randomUUID(), userId, "RESP-CF", UUID.randomUUID(), Instant.now(), Instant.now());
        supportResponseRepository.create(srsp);

        String url = "http://localhost:" + port + "/api/blogposts/download/" + userId + "?format=json&contentType=blogpost";
        ResponseEntity<byte[]> resp = restTemplate.getForEntity(url, byte[].class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        ExportResponse parsed = objectMapper.readValue(resp.getBody(), ExportResponse.class);
        assertNotNull(parsed);
        assertFalse(parsed.getBlogPosts().isEmpty());
        assertTrue(parsed.getSupportRequests().isEmpty());
        assertTrue(parsed.getSupportResponses().isEmpty());
    }

    @Test
    void singleUser_invalidContentType_returns400() {
        String url = "http://localhost:" + port + "/api/blogposts/download/" + userId + "?contentType=notype";
        ResponseEntity<String> resp = restTemplate.getForEntity(url, String.class);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertTrue(resp.getBody().toLowerCase().contains("invalid contenttype"));
    }

    @Test
    void bulk_post_json_returns_only_selected_blogposts_and_filename_bulk_pattern() throws Exception {
        BlogPost b1 = new BlogPost(UUID.randomUUID(), "BULK1", "b", Instant.now(), Instant.now(), userId);
        BlogPost b2 = new BlogPost(UUID.randomUUID(), "BULK2", "b", Instant.now(), Instant.now(), userId);
        blogPostRepository.createBlogPost(b1);
        blogPostRepository.createBlogPost(b2);

        List<UUID> ids = List.of(b2.getId());
        String url = "http://localhost:" + port + "/api/blogposts/download/bulk?format=json&contentType=blogpost";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(ids.stream().map(UUID::toString).collect(Collectors.toList())), headers);

        ResponseEntity<byte[]> resp = restTemplate.postForEntity(url, entity, byte[].class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, resp.getHeaders().getContentType());

        String cd = resp.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        assertNotNull(cd);
        assertTrue(cd.contains("export-bulk-users"));
        assertTrue(cd.toLowerCase().contains(".json"));

        ExportResponse parsed = objectMapper.readValue(resp.getBody(), ExportResponse.class);
        assertNotNull(parsed);
        assertEquals(1, parsed.getBlogPosts().size());
        assertEquals(b2.getId(), parsed.getBlogPosts().get(0).getId());
    }

    @Test
    void bulk_post_csv_and_xml_filename_and_content_checks() throws Exception {
        SupportRequest s1 = new SupportRequest(UUID.randomUUID(), userId, "SR-B1", UUID.randomUUID(), UUID.randomUUID(), Instant.now(), Instant.now());
        supportRequestRepository.create(s1);

        List<UUID> ids = List.of(userId);
        // CSV
        String csvUrl = "http://localhost:" + port + "/api/blogposts/download/bulk?format=csv&contentType=supportrequest";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(ids.stream().map(UUID::toString).collect(Collectors.toList())), headers);

        ResponseEntity<byte[]> csvResp = restTemplate.postForEntity(csvUrl, entity, byte[].class);
        assertEquals(HttpStatus.OK, csvResp.getStatusCode());
        assertEquals(MediaType.valueOf("text/csv"), csvResp.getHeaders().getContentType());
        String csvCd = csvResp.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        assertNotNull(csvCd);
        assertTrue(csvCd.contains("export-bulk-users"));
        assertTrue(csvCd.toLowerCase().contains(".csv"));
        String csvBody = new String(csvResp.getBody(), StandardCharsets.UTF_8);
        assertTrue(csvBody.contains("# SupportRequests"));
        assertTrue(csvBody.contains("SR-B1"));

        // XML
        String xmlUrl = "http://localhost:" + port + "/api/blogposts/download/bulk?format=xml&contentType=supportrequest";
        ResponseEntity<byte[]> xmlResp = restTemplate.postForEntity(xmlUrl, entity, byte[].class);
        assertEquals(HttpStatus.OK, xmlResp.getStatusCode());
        assertEquals(MediaType.APPLICATION_XML, xmlResp.getHeaders().getContentType());
        String xmlCd = xmlResp.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        assertNotNull(xmlCd);
        assertTrue(xmlCd.contains("export-bulk-users"));
        assertTrue(xmlCd.toLowerCase().contains(".xml"));
        String xmlBody = new String(xmlResp.getBody(), StandardCharsets.UTF_8);
        assertTrue(xmlBody.contains("<supportRequests>"));
        assertTrue(xmlBody.contains("<text>SR-B1</text>"));
    }

    @Test
    void bulk_post_malformedIds_ignores_invalid_and_exports_valid() throws Exception {
        BlogPost b1 = new BlogPost(UUID.randomUUID(), "MB1", "b", Instant.now(), Instant.now(), userId);
        blogPostRepository.createBlogPost(b1);

        List<String> bodyList = List.of("not-a-uuid", b1.getId().toString());
        String url = "http://localhost:" + port + "/api/blogposts/download/bulk?format=json&contentType=blogpost";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(bodyList), headers);

        ResponseEntity<byte[]> resp = restTemplate.postForEntity(url, entity, byte[].class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());

        ExportResponse parsed = objectMapper.readValue(resp.getBody(), ExportResponse.class);
        assertNotNull(parsed);
        assertEquals(1, parsed.getBlogPosts().size());
        assertEquals(b1.getId(), parsed.getBlogPosts().get(0).getId());
    }

    @Test
    void bulk_post_emptyList_returnsEmptyExport_but_200() throws Exception {
        List<String> bodyList = List.of();
        String url = "http://localhost:" + port + "/api/blogposts/download/bulk?format=json&contentType=blogpost";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(bodyList), headers);

        ResponseEntity<byte[]> resp = restTemplate.postForEntity(url, entity, byte[].class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());

        ExportResponse parsed = objectMapper.readValue(resp.getBody(), ExportResponse.class);
        assertNotNull(parsed);
        assertTrue(parsed.getBlogPosts().isEmpty());
    }

    @Test
    void bulk_post_invalidContentType_returns400() throws Exception {
        List<String> bodyList = List.of(UUID.randomUUID().toString());
        String url = "http://localhost:" + port + "/api/blogposts/download/bulk?format=json&contentType=invalidType";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(bodyList), headers);

        ResponseEntity<String> resp = restTemplate.postForEntity(url, entity, String.class);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertTrue(resp.getBody().toLowerCase().contains("invalid contenttype"));
    }

    @Test
    void caseInsensitive_format_and_contentType_handling() throws Exception {
        BlogPost bp = new BlogPost(UUID.randomUUID(), "CaseI", "body", Instant.now(), Instant.now(), userId);
        blogPostRepository.createBlogPost(bp);

        String url = "http://localhost:" + port + "/api/blogposts/download/" + userId + "?format=CSV&contentType=BlogPost";
        ResponseEntity<byte[]> resp = restTemplate.getForEntity(url, byte[].class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(MediaType.valueOf("text/csv"), resp.getHeaders().getContentType());
        String cd = resp.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        assertNotNull(cd);
        assertTrue(cd.contains("export-" + userId.toString()) || cd.contains("export-bulk"));

        // content-length header should be present and equal to body length
        long len = resp.getHeaders().getContentLength();
        byte[] body = resp.getBody();
        assertNotNull(body);
        assertEquals(body.length, len);
    }
}