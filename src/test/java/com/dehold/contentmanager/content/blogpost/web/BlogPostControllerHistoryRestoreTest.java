package com.dehold.contentmanager.content.blogpost.web;


import com.dehold.contentmanager.content.blogpost.model.BlogPost;
import com.dehold.contentmanager.content.blogpost.model.BlogPostHistory;
import com.dehold.contentmanager.content.blogpost.service.BlogPostService;
import com.dehold.contentmanager.exception.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BlogPostControllerHistoryRestoreTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BlogPostService blogPostService;

    @Test
    void getBlogPostHistory_shouldReturnHistoryList() throws Exception {
        UUID id = UUID.randomUUID();

        List<BlogPostHistory> mockHistory = List.of(
                new BlogPostHistory(UUID.randomUUID(), id, "Title v1", "Content v1", 1, Instant.now(), Instant.now()),
                new BlogPostHistory(UUID.randomUUID(), id, "Title v2", "Content v2", 2, Instant.now(), Instant.now())
        );

        Mockito.when(blogPostService.getHistory(id)).thenReturn(mockHistory);

        mockMvc.perform(get("/api/blogposts/{id}/history", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].versionNumber").value(1))
                .andExpect(jsonPath("$[1].versionNumber").value(2));
    }

    @Test
    void getBlogPostHistory_whenNotFound_shouldReturn404() throws Exception {
        UUID id = UUID.randomUUID();

        Mockito.when(blogPostService.getHistory(id))
                .thenThrow(EntityNotFoundException.of("BlogPost", id.toString()));

        mockMvc.perform(get("/api/blogposts/{id}/history", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void restoreBlogPostVersion_shouldReturnRestoredPost() throws Exception {
        UUID id = UUID.randomUUID();
        int version = 2;

        BlogPost restored = new BlogPost(
                id,
                "Restored Title",
                "Restored Content",
                Instant.now(),
                Instant.now(),
                UUID.randomUUID()
        );

        Mockito.when(blogPostService.restoreVersion(id, version))
                .thenReturn(restored);

        mockMvc.perform(post("/api/blogposts/{id}/restore/{version}", id, version))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Restored Title"))
                .andExpect(jsonPath("$.content").value("Restored Content"));
    }

    @Test
    void restoreBlogPostVersion_whenVersionNotFound_shouldReturn404() throws Exception {
        UUID id = UUID.randomUUID();
        int version = 999;

        Mockito.when(blogPostService.restoreVersion(id, version))
                .thenThrow(EntityNotFoundException.of("BlogPostVersion", "" + version));

        mockMvc.perform(post("/api/blogposts/{id}/restore/{version}", id, version))
                .andExpect(status().isNotFound());
    }

    @Test
    void restoreBlogPostVersion_shouldCallServiceWithCorrectArguments() throws Exception {
        UUID id = UUID.randomUUID();
        int version = 3;

        BlogPost restored = new BlogPost(
                id, "Title", "Content", Instant.now(), Instant.now(), UUID.randomUUID()
        );

        Mockito.when(blogPostService.restoreVersion(any(UUID.class), any(Integer.class)))
                .thenReturn(restored);

        mockMvc.perform(post("/api/blogposts/{id}/restore/{version}", id, version))
                .andExpect(status().isOk());

        Mockito.verify(blogPostService).restoreVersion(eq(id), eq(version));
    }
}
