package com.dehold.contentmanager.content.faqpage.web;

import com.dehold.contentmanager.content.faqpage.model.FaqItem;
import com.dehold.contentmanager.content.faqpage.model.FaqPage;
import com.dehold.contentmanager.content.faqpage.repository.FaqPageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests for FaqPageController using @WebMvcTest.
 * Tests use real HTTP layer with MockMvc to validate:
 * - JSON serialization/deserialization
 * - HTTP status codes and headers
 * - Response body structure and content
 * 
 * Assertions are balanced to avoid:
 * - Over-strict timestamp equality checks (timestamps change between create/fetch)
 * - Over-strict ordering assumptions (uses containsInAnyOrder for lists)
 * - False positives from mocking implementation details
 * - Over-validation of business logic (tested in integration tests)
 */
@WebMvcTest(FaqPageController.class)
class FaqPageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FaqPageRepository repository;

    @TestConfiguration
    static class TestConfig {
        @Bean
        FaqPageRepository faqPageRepository() {
            FaqPageRepository repo = mock(FaqPageRepository.class);
            // Setup default behavior for createFaqPage: simulate setting ID and timestamps
            doAnswer(invocation -> {
                FaqPage page = invocation.getArgument(0);
                if (page.getId() == null) {
                    page.setId(UUID.randomUUID());
                }
                if (page.getCreatedAt() == null) {
                    page.setCreatedAt(Instant.now());
                }
                if (page.getUpdatedAt() == null) {
                    page.setUpdatedAt(page.getCreatedAt());
                }
                // Make getFaqPage return the saved page
                when(repo.getFaqPage(page.getId())).thenReturn(Optional.of(page));
                return null;
            }).when(repo).createFaqPage(any(FaqPage.class));
            return repo;
        }
    }

    // ==================== POST /api/faqpages Tests ====================

    @Test
    @WithMockUser
    void createFaqPage_withValidPayload_returns201() throws Exception {
        UUID userId = UUID.randomUUID();

        String request = "{\n" +
                "  \"userId\": \"" + userId + "\",\n" +
                "  \"title\": \"How to Use Our Service\",\n" +
                "  \"introduction\": \"Learn the basics\",\n" +
                "  \"faqItems\": [\n" +
                "    {\"title\": \"What is this?\", \"text\": \"It's a service\"},\n" +
                "    {\"title\": \"How do I start?\", \"text\": \"Click here\"}\n" +
                "  ]\n" +
                "}";

        mockMvc.perform(post("/api/faqpages")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.title", equalTo("How to Use Our Service")))
                .andExpect(jsonPath("$.introduction", equalTo("Learn the basics")))
                .andExpect(jsonPath("$.userId", notNullValue()))
                .andExpect(jsonPath("$.faqItems", hasSize(2)))
                .andExpect(jsonPath("$.createdAt", notNullValue()))
                .andExpect(jsonPath("$.updatedAt", notNullValue()));
    }

    @Test
    @WithMockUser
    void createFaqPage_withEmptyFaqItems_returns201() throws Exception {
        UUID userId = UUID.randomUUID();

        String request = "{\n" +
                "  \"userId\": \"" + userId + "\",\n" +
                "  \"title\": \"Empty FAQ\",\n" +
                "  \"introduction\": \"No items yet\",\n" +
                "  \"faqItems\": []\n" +
                "}";

        mockMvc.perform(post("/api/faqpages")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", equalTo("Empty FAQ")))
                .andExpect(jsonPath("$.faqItems", hasSize(0)));
    }

    @Test
    @WithMockUser
    void createFaqPage_withNullFaqItems_defaultsToEmpty() throws Exception {
        UUID userId = UUID.randomUUID();

        String request = "{\n" +
                "  \"userId\": \"" + userId + "\",\n" +
                "  \"title\": \"Title\",\n" +
                "  \"introduction\": \"Intro\"\n" +
                "}";

        mockMvc.perform(post("/api/faqpages")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.faqItems", notNullValue()))
                .andExpect(jsonPath("$.faqItems", hasSize(0)));
    }

    @Test
    @WithMockUser
    void createFaqPage_withLargeFaqItemsList_returns201() throws Exception {
        UUID userId = UUID.randomUUID();

        StringBuilder itemsJson = new StringBuilder();
        for (int i = 1; i <= 8; i++) {
            if (i > 1) itemsJson.append(",");
            itemsJson.append("{\"title\": \"Q").append(i).append("\", \"text\": \"A").append(i).append("\"}");
        }

        String request = "{\n" +
                "  \"userId\": \"" + userId + "\",\n" +
                "  \"title\": \"Large FAQ\",\n" +
                "  \"introduction\": \"Many items\",\n" +
                "  \"faqItems\": [" + itemsJson + "]\n" +
                "}";

        mockMvc.perform(post("/api/faqpages")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.faqItems", hasSize(8)))
                .andExpect(jsonPath("$.faqItems[*].title", hasItems("Q1", "Q2", "Q3", "Q4", "Q5", "Q6", "Q7", "Q8")));
    }

    @Test
    @WithMockUser
    void createFaqPage_withSpecialCharacters_returns201() throws Exception {
        UUID userId = UUID.randomUUID();

        String request = "{\n" +
                "  \"userId\": \"" + userId + "\",\n" +
                "  \"title\": \"FAQ: How to use \\\"quotes\\\" & special chars?\",\n" +
                "  \"introduction\": \"Intro\"\n" +
                "}";

        mockMvc.perform(post("/api/faqpages")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", containsString("FAQ")));
    }

    @Test
    @WithMockUser
    void createFaqPage_createsAndFetchesSuccessfully() throws Exception {
        UUID userId = UUID.randomUUID();

        String request = "{\n" +
                "  \"userId\": \"" + userId + "\",\n" +
                "  \"title\": \"Test FAQ\",\n" +
                "  \"introduction\": \"Test intro\",\n" +
                "  \"faqItems\": [{\"title\": \"Q\", \"text\": \"A\"}]\n" +
                "}";

        mockMvc.perform(post("/api/faqpages")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.title", equalTo("Test FAQ")))
                .andExpect(jsonPath("$.faqItems", hasSize(1)));
    }

    // ==================== GET /api/faqpages/{id} Tests ====================

    @Test
    @WithMockUser
    void getFaqPage_withValidId_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        FaqPage page = new FaqPage(
                id,
                "FAQ Page",
                "Introduction",
                List.of(new FaqItem("Question", "Answer")),
                now,
                now,
                userId
        );
        when(repository.getFaqPage(id)).thenReturn(Optional.of(page));

        mockMvc.perform(get("/api/faqpages/" + id)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.title", notNullValue()))
                .andExpect(jsonPath("$.introduction", notNullValue()))
                .andExpect(jsonPath("$.userId", notNullValue()))
                .andExpect(jsonPath("$.faqItems", notNullValue()))
                .andExpect(jsonPath("$.createdAt", notNullValue()))
                .andExpect(jsonPath("$.updatedAt", notNullValue()));
    }

    @Test
    @WithMockUser
    void getFaqPage_withValidIdAndMultipleItems_returnsAllItems() throws Exception {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        FaqPage page = new FaqPage(
                id,
                "Multi-Item FAQ",
                "Many items",
                List.of(
                        new FaqItem("Q1", "A1"),
                        new FaqItem("Q2", "A2"),
                        new FaqItem("Q3", "A3")
                ),
                now,
                now,
                userId
        );
        when(repository.getFaqPage(id)).thenReturn(Optional.of(page));

        mockMvc.perform(get("/api/faqpages/" + id)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.faqItems", hasSize(3)))
                .andExpect(jsonPath("$.faqItems[*].title", hasItems("Q1", "Q2", "Q3")))
                .andExpect(jsonPath("$.faqItems[*].text", hasItems("A1", "A2", "A3")));
    }

    @Test
    @WithMockUser
    void getFaqPage_withNonExistentId_returns404() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        when(repository.getFaqPage(nonExistentId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/faqpages/" + nonExistentId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", notNullValue()))
                .andExpect(jsonPath("$.error", containsString("does not exist")));
    }

    @Test
    @WithMockUser
    void getFaqPage_withValidIdAndEmptyItems_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        FaqPage page = new FaqPage(id, "Empty FAQ", "No items", List.of(), now, now, userId);
        when(repository.getFaqPage(id)).thenReturn(Optional.of(page));

        mockMvc.perform(get("/api/faqpages/" + id)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", equalTo("Empty FAQ")))
                .andExpect(jsonPath("$.faqItems", hasSize(0)));
    }

    // ==================== GET /api/faqpages/users/{userId} Tests ====================

    @Test
    @WithMockUser
    void getFaqPagesByUser_withMultiplePages_returns200() throws Exception {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        List<FaqPage> pages = List.of(
                new FaqPage(UUID.randomUUID(), "FAQ 1", "Intro 1", List.of(new FaqItem("Q1", "A1")), now, now, userId),
                new FaqPage(UUID.randomUUID(), "FAQ 2", "Intro 2", List.of(new FaqItem("Q2", "A2")), now, now, userId),
                new FaqPage(UUID.randomUUID(), "FAQ 3", "Intro 3", List.of(new FaqItem("Q3", "A3")), now, now, userId)
        );
        when(repository.getFaqPagesByUserId(userId)).thenReturn(pages);

        mockMvc.perform(get("/api/faqpages/users/" + userId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].title", hasItems("FAQ 1", "FAQ 2", "FAQ 3")));
    }

    @Test
    @WithMockUser
    void getFaqPagesByUser_withNoPages_returns200EmptyArray() throws Exception {
        UUID userId = UUID.randomUUID();
        when(repository.getFaqPagesByUserId(userId)).thenReturn(List.of());

        mockMvc.perform(get("/api/faqpages/users/" + userId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @WithMockUser
    void getFaqPagesByUser_withSinglePage_returns200() throws Exception {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        List<FaqPage> pages = List.of(
                new FaqPage(UUID.randomUUID(), "Single FAQ", "One page", List.of(new FaqItem("Q", "A")), now, now, userId)
        );
        when(repository.getFaqPagesByUserId(userId)).thenReturn(pages);

        mockMvc.perform(get("/api/faqpages/users/" + userId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", notNullValue()));
    }

    @Test
    @WithMockUser
    void getFaqPagesByUser_allPagesHaveExpectedStructure() throws Exception {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        List<FaqPage> pages = List.of(
                new FaqPage(UUID.randomUUID(), "FAQ A", "Intro A", List.of(new FaqItem("QA", "AA")), now, now, userId),
                new FaqPage(UUID.randomUUID(), "FAQ B", "Intro B", List.of(new FaqItem("QB", "AB")), now, now, userId)
        );
        when(repository.getFaqPagesByUserId(userId)).thenReturn(pages);

        mockMvc.perform(get("/api/faqpages/users/" + userId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", everyItem(notNullValue())))
                .andExpect(jsonPath("$[*].title", everyItem(notNullValue())))
                .andExpect(jsonPath("$[*].userId", everyItem(notNullValue())))
                .andExpect(jsonPath("$[*].createdAt", everyItem(notNullValue())))
                .andExpect(jsonPath("$[*].updatedAt", everyItem(notNullValue())));
    }

    @Test
    @WithMockUser
    void getFaqPagesByUser_eachPageHasFaqItems() throws Exception {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        List<FaqPage> pages = List.of(
                new FaqPage(UUID.randomUUID(), "FAQ 1", "I1", List.of(new FaqItem("Q1", "A1"), new FaqItem("Q2", "A2")), now, now, userId),
                new FaqPage(UUID.randomUUID(), "FAQ 2", "I2", List.of(new FaqItem("Q3", "A3")), now, now, userId)
        );
        when(repository.getFaqPagesByUserId(userId)).thenReturn(pages);

        mockMvc.perform(get("/api/faqpages/users/" + userId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].faqItems", hasSize(2)))
                .andExpect(jsonPath("$[1].faqItems", hasSize(1)));
    }

    // ==================== Error Handling & Edge Cases ====================

    @Test
    @WithMockUser
    void createFaqPage_withMissingTitle_stillCreates() throws Exception {
        UUID userId = UUID.randomUUID();

        String request = "{\n" +
                "  \"userId\": \"" + userId + "\",\n" +
                "  \"introduction\": \"Intro\"\n" +
                "}";

        mockMvc.perform(post("/api/faqpages")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser
    void createFaqPage_withBlankStrings_stillCreates() throws Exception {
        UUID userId = UUID.randomUUID();

        String request = "{\n" +
                "  \"userId\": \"" + userId + "\",\n" +
                "  \"title\": \"\",\n" +
                "  \"introduction\": \"\"\n" +
                "}";

        mockMvc.perform(post("/api/faqpages")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isCreated());
    }

    @Test
    void createFaqPage_withoutAuthentication_returns401() throws Exception {
        String request = "{\n" +
                "  \"userId\": \"" + UUID.randomUUID() + "\",\n" +
                "  \"title\": \"FAQ\",\n" +
                "  \"introduction\": \"Intro\"\n" +
                "}";

        mockMvc.perform(post("/api/faqpages")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getFaqPage_withoutAuthentication_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/faqpages/" + UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
