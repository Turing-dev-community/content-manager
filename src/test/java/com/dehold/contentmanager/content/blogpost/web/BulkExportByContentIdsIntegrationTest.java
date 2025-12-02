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
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class BulkExportByContentIdsIntegrationTest extends ContentManagerApplicationTests {

    @LocalServerPort private int port;
    @Autowired private TestRestTemplate restTemplate;
    @Autowired private BlogPostRepository blogPostRepository;
    @Autowired private SupportRequestRepository supportRequestRepository;
    @Autowired private SupportResponseRepository supportResponseRepository;
    @Autowired private UserService userService;
    @Autowired private ObjectMapper objectMapper;

    private UUID uid;

    @BeforeEach
    void setup() {
        CreateUserRequest req = new CreateUserRequest();
        req.setAlias("bulk-content-user");
        req.setEmail("bulk-content+" + UUID.randomUUID().toString().substring(0,8) + "@example.com");
        req.setUsername("TestUser" + UUID.randomUUID().toString().substring(0,8));
        req.setPassword("TestPassword" + UUID.randomUUID().toString().substring(0,8));
        User u = userService.createUser(req);
        uid = u.getId();
    }

    @Test
    void bulkByContentIds_json_exportsOnlySpecifiedBlogPosts() throws Exception {
        BlogPost b1 = new BlogPost(UUID.randomUUID(), "Bc1", "b", Instant.now(), Instant.now(), uid);
        BlogPost b2 = new BlogPost(UUID.randomUUID(), "Bc2", "b", Instant.now(), Instant.now(), uid);
        blogPostRepository.createBlogPost(b1);
        blogPostRepository.createBlogPost(b2);

        List<String> ids = List.of(b2.getId().toString());
        String url = "http://localhost:" + port + "/api/users/export-by-content-ids?format=json&contentType=blogpost";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(ids), headers);

        ResponseEntity<byte[]> resp = restTemplate.postForEntity(url, entity, byte[].class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, resp.getHeaders().getContentType());

        ExportResponse parsed = objectMapper.readValue(resp.getBody(), ExportResponse.class);
        assertNotNull(parsed);
        assertEquals(1, parsed.getBlogPosts().size());
        assertEquals(b2.getId(), parsed.getBlogPosts().get(0).getId());
    }

    @Test
    void bulkByContentIds_csv_supportRequest_onlySelected() throws Exception {
        SupportRequest s1 = new SupportRequest(UUID.randomUUID(), uid, "SR1", UUID.randomUUID(), UUID.randomUUID(), Instant.now(), Instant.now());
        SupportRequest s2 = new SupportRequest(UUID.randomUUID(), uid, "SR2", UUID.randomUUID(), UUID.randomUUID(), Instant.now(), Instant.now());
        supportRequestRepository.create(s1);
        supportRequestRepository.create(s2);

        List<String> ids = List.of(s1.getId().toString());
        String url = "http://localhost:" + port + "/api/users/export-by-content-ids?format=csv&contentType=supportrequest";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(ids), headers);

        ResponseEntity<byte[]> resp = restTemplate.postForEntity(url, entity, byte[].class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(MediaType.valueOf("text/csv"), resp.getHeaders().getContentType());
        String csv = new String(resp.getBody(), StandardCharsets.UTF_8);
        assertTrue(csv.contains("SR1"));
        assertFalse(csv.contains("SR2"));
    }

    @Test
    void bulkByContentIds_xml_supportResponse_onlySelected() throws Exception {
        SupportRequest sreq = new SupportRequest(UUID.randomUUID(), uid, "SRx", UUID.randomUUID(), UUID.randomUUID(), Instant.now(), Instant.now());
        supportRequestRepository.create(sreq);
        SupportResponse r1 = new SupportResponse(UUID.randomUUID(), uid, "R1", sreq.getId(), Instant.now(), Instant.now());
        SupportResponse r2 = new SupportResponse(UUID.randomUUID(), uid, "R2", sreq.getId(), Instant.now(), Instant.now());

        supportResponseRepository.create(r1);
        supportResponseRepository.create(r2);

        List<String> ids = List.of(r2.getId().toString());
        String url = "http://localhost:" + port + "/api/users/export-by-content-ids?format=xml&contentType=supportresponse";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(ids), headers);

        ResponseEntity<byte[]> resp = restTemplate.postForEntity(url, entity, byte[].class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(MediaType.APPLICATION_XML, resp.getHeaders().getContentType());
        String xml = new String(resp.getBody(), StandardCharsets.UTF_8);
        assertTrue(xml.contains("<supportResponses>"));
        assertTrue(xml.contains("R2"));
        assertFalse(xml.contains("R1"));
    }

    @Test
    void bulkByContentIds_malformedIds_ignoresBadAndExportsGood() throws Exception {
        BlogPost b1 = new BlogPost(UUID.randomUUID(), "Mb1", "b", Instant.now(), Instant.now(), uid);
        blogPostRepository.createBlogPost(b1);
        UUID unknownId = UUID.randomUUID();
        List<String> ids = List.of(unknownId.toString(), b1.getId().toString());
        String url = "http://localhost:" + port + "/api/users/export-by-content-ids?format=json&contentType=blogpost";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(ids), headers);

        ResponseEntity<byte[]> resp = restTemplate.postForEntity(url, entity, byte[].class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        ExportResponse parsed = objectMapper.readValue(resp.getBody(), ExportResponse.class);
        assertEquals(1, parsed.getBlogPosts().size());
        assertEquals(b1.getId(), parsed.getBlogPosts().get(0).getId());
    }

    @Test
    void bulkByContentIds_emptyList_returnsEmptyExport() throws Exception {
        List<String> ids = List.of();
        String url = "http://localhost:" + port + "/api/users/export-by-content-ids?format=json&contentType=blogpost";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(ids), headers);

        ResponseEntity<byte[]> resp = restTemplate.postForEntity(url, entity, byte[].class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        ExportResponse parsed = objectMapper.readValue(resp.getBody(), ExportResponse.class);
        assertTrue(parsed.getBlogPosts().isEmpty());
    }

    @Test
    void bulkByContentIds_invalidContentType() throws Exception {
        List<String> ids = List.of(UUID.randomUUID().toString());
        String url = "http://localhost:" + port + "/api/users/export-by-content-ids?format=json&contentType=invalid";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(ids), headers);

        ResponseEntity<String> resp = restTemplate.postForEntity(url, entity, String.class);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertTrue(resp.getBody().toLowerCase().contains("invalid contenttype"));
    }
}

