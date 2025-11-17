package com.dehold.contentmanager.validation.web;

import com.dehold.contentmanager.exception.CustomErrorResponse;
import com.dehold.contentmanager.validation.model.ForbiddenWords;
import com.dehold.contentmanager.validation.repository.ForbiddenWordsRepository;
import com.dehold.contentmanager.validation.web.dto.ForbiddenWordsUpdateDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ForbiddenWordsControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ForbiddenWordsRepository forbiddenWordsRepository;

    @BeforeEach
    void setup() {
        jdbcTemplate.update("DELETE FROM forbidden_words");
    }

    @Test
    void createForbiddenWords_shouldReturnCreatedForbiddenWords() {
        LinkedHashSet<String> words = new LinkedHashSet<>();
        words.add("badword1");
        words.add("inappropriate");

        ForbiddenWords request = new ForbiddenWords(
                null,
                UUID.randomUUID(),
                "Test forbidden words",
                "blogpost",
                "content",
                words
        );

        ResponseEntity<ForbiddenWords> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/forbidden-words",
                request,
                ForbiddenWords.class
        );

        assertEquals(201, response.getStatusCode().value());
        ForbiddenWords forbiddenWordsResponse = response.getBody();
        assertNotNull(forbiddenWordsResponse);
        assertEquals(request.getDescription(), forbiddenWordsResponse.getDescription());
        assertEquals(request.getContentType(), forbiddenWordsResponse.getContentType());
        assertEquals(request.getFieldName(), forbiddenWordsResponse.getFieldName());
        assertEquals(2, forbiddenWordsResponse.getWords().size());
        assertTrue(forbiddenWordsResponse.getWords().contains("badword1"));
        assertTrue(forbiddenWordsResponse.getWords().contains("inappropriate"));
    }

    @Test
    void getForbiddenWordsById_shouldReturnForbiddenWords() {
        LinkedHashSet<String> words = new LinkedHashSet<>();
        words.add("testword");

        ForbiddenWords forbiddenWords = new ForbiddenWords(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Integration test words",
                "comment",
                "text",
                words
        );
        forbiddenWordsRepository.save(forbiddenWords);

        ResponseEntity<ForbiddenWords> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/forbidden-words/" + forbiddenWords.getId(),
                ForbiddenWords.class
        );

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(forbiddenWords.getDescription(), response.getBody().getDescription());
        assertEquals(forbiddenWords.getContentType(), response.getBody().getContentType());
    }

    @Test
    void updateForbiddenWords_shouldReturnUpdatedForbiddenWords() {
        LinkedHashSet<String> originalWords = new LinkedHashSet<>();
        originalWords.add("oldword");

        ForbiddenWords original = new ForbiddenWords(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Original description",
                "blogpost",
                "title",
                originalWords
        );
        forbiddenWordsRepository.save(original);

        LinkedHashSet<String> updatedWords = new LinkedHashSet<>(originalWords);
        updatedWords.add("newword1");
        updatedWords.add("newword2");

        ForbiddenWordsUpdateDto updateRequest = new ForbiddenWordsUpdateDto(
                original.getId(),
                original.getUserId(),
                "Updated description",
                "comment",
                "content",
                updatedWords
        );

        HttpEntity<ForbiddenWordsUpdateDto> entity = new HttpEntity<>(updateRequest);
        ResponseEntity<ForbiddenWords> response = restTemplate.exchange(
                "http://localhost:" + port + "/api/forbidden-words/" + original.getId(),
                HttpMethod.PUT,
                entity,
                ForbiddenWords.class
        );

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(original.getId(), response.getBody().getId());
        assertEquals(updateRequest.getDescription(), response.getBody().getDescription());
        assertEquals(updateRequest.getContentType(), response.getBody().getContentType());
        LinkedHashSet<String> updatedWordsResponse = response.getBody().getWords();
        assertEquals(3, updatedWords.size());
        assertTrue(updatedWordsResponse.contains("oldword"));
        assertTrue(updatedWordsResponse.contains("newword1"));
        assertTrue(updatedWordsResponse.contains("newword2"));
    }

    @Test
    void updateForbiddenWords_shouldReturnBadRequestWhenFieldsAreMissing() {
        LinkedHashSet<String> originalWords = new LinkedHashSet<>();
        originalWords.add("originalword");

        ForbiddenWords original = new ForbiddenWords(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Original description",
                "blogpost",
                "title",
                originalWords
        );
        forbiddenWordsRepository.save(original);


        ForbiddenWordsUpdateDto updateRequest = new ForbiddenWordsUpdateDto(
                null,
                null,
                "Updated description",
                null,
                "content",
                null
        );

        HttpEntity<ForbiddenWordsUpdateDto> entity = new HttpEntity<>(updateRequest);
        ResponseEntity<CustomErrorResponse> response = restTemplate.exchange(
                "http://localhost:" + port + "/api/forbidden-words/" + original.getId(),
                HttpMethod.PUT,
                entity,
                CustomErrorResponse.class
        );

        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getError().contains("The following fields must not be null"));
        assertTrue(response.getBody().getError().contains("id"));
        assertTrue(response.getBody().getError().contains("userId"));
        assertTrue(response.getBody().getError().contains("contentType"));
        assertTrue(response.getBody().getError().contains("words"));
        assertFalse(response.getBody().getError().contains("description"));
        assertFalse(response.getBody().getError().contains("fieldName"));
        assertTrue(response.getBody().getPath().contains("/api/forbidden-words/" + original.getId()));
    }

    @Test
    void updateForbiddenWords_shouldReturnBadRequestWhenPathIdDoesNotMatchPayloadId() {
        LinkedHashSet<String> originalWords = new LinkedHashSet<>();
        originalWords.add("originalword");

        ForbiddenWords original = new ForbiddenWords(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Original description",
                "blogpost",
                "title",
                originalWords
        );
        forbiddenWordsRepository.save(original);

        LinkedHashSet<String> updatedWords = new LinkedHashSet<>(originalWords);
        updatedWords.add("newword");

        // Create DTO with different ID than the path parameter
        UUID differentId = UUID.randomUUID();
        ForbiddenWordsUpdateDto updateRequest = new ForbiddenWordsUpdateDto(
                differentId, // different ID than path parameter
                original.getUserId(),
                "Updated description",
                "comment",
                "content",
                updatedWords
        );

        HttpEntity<ForbiddenWordsUpdateDto> entity = new HttpEntity<>(updateRequest);
        ResponseEntity<CustomErrorResponse> response = restTemplate.exchange(
                "http://localhost:" + port + "/api/forbidden-words/" + original.getId(),
                HttpMethod.PUT,
                entity,
                CustomErrorResponse.class
        );

        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("The path ID does not match payload ID.", response.getBody().getError());
        assertTrue(response.getBody().getPath().contains("/api/forbidden-words/" + original.getId()));
    }



    @Test
    void getForbiddenWords_shouldReturnNotFound() {
        UUID nonExistentId = UUID.randomUUID();

        ResponseEntity<CustomErrorResponse> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/forbidden-words/" + nonExistentId,
                CustomErrorResponse.class
        );

        assertNotNull(response.getBody());
        CustomErrorResponse errorResponse = response.getBody();
        assertEquals(404, errorResponse.getHttpStatusCode());
        assertEquals("The entity ForbiddenWords with id " + nonExistentId + " does not exist", errorResponse.getError());
    }

    @Test
    void getAllForbiddenWords_shouldReturnForbiddenWordsListIncludingDefaultWords() {
        LinkedHashSet<String> words = new LinkedHashSet<>();
        words.add("listword");

        ForbiddenWords forbiddenWords = new ForbiddenWords(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "List test words",
                "any",
                "any",
                words
        );
        forbiddenWordsRepository.save(forbiddenWords);

        ResponseEntity<ForbiddenWords[]> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/forbidden-words",
                ForbiddenWords[].class
        );

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        List<ForbiddenWords> forbiddenWordsResponse = List.of(response.getBody());
        assertNotNull(forbiddenWordsResponse);
        assertEquals(2, forbiddenWordsResponse.size());
        ForbiddenWords forbiddenWordsDefault =
                forbiddenWordsResponse.stream().filter(forbidden -> forbidden.getDescription().contains("Default " +
                        "Forbidden " +
                        "Words provided by the system.")).findFirst().orElse(null);
        assertNotNull(forbiddenWordsDefault);
        ForbiddenWords forbiddenWordsDCustom =
                forbiddenWordsResponse.stream().filter(forbidden -> forbidden.getDescription().contains("List test words")).findFirst().orElse(null);
        assertNotNull(forbiddenWordsDCustom);
        assertEquals(1, forbiddenWordsDCustom.getWords().size());
        assertEquals("listword", forbiddenWordsDCustom.getWords().getFirst());
    }

    @Test
    void deleteForbiddenWords_shouldReturnNoContent() {
        LinkedHashSet<String> words = new LinkedHashSet<>();
        words.add("deleteword");

        ForbiddenWords forbiddenWords = new ForbiddenWords(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Delete test words",
                "blogpost",
                "content",
                words
        );
        forbiddenWordsRepository.save(forbiddenWords);

        ResponseEntity<Void> response = restTemplate.exchange(
                "http://localhost:" + port + "/api/forbidden-words/" + forbiddenWords.getId(),
                HttpMethod.DELETE,
                null,
                Void.class
        );

        assertEquals(204, response.getStatusCode().value());
    }
}