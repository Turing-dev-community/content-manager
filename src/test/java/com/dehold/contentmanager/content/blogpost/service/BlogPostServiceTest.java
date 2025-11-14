package com.dehold.contentmanager.content.blogpost.service;

import com.dehold.contentmanager.content.blogpost.model.BlogPost;
import com.dehold.contentmanager.content.blogpost.repository.BlogPostRepository;
import com.dehold.contentmanager.content.blogpost.web.dto.CreateBlogPostRequest;
import com.dehold.contentmanager.content.blogpost.web.dto.UpdateBlogPostRequest;
import com.dehold.contentmanager.content.blogpost.model.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class BlogPostServiceTest {

    @Mock
    private BlogPostRepository blogPostRepository;

    @InjectMocks
    private BlogPostService blogPostService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createBlogPost_shouldCreateAndReturnBlogPost() {
        CreateBlogPostRequest request = new CreateBlogPostRequest();
        request.setTitle("Test Blog Post");
        request.setContent("This is a test blog post.");
        UUID userId = UUID.randomUUID();
        request.setUserId(userId);

        BlogPost blogPost = new BlogPost(UUID.randomUUID(), "Test Blog Post", "This is a test blog post.",
                Instant.now(), Instant.now(), UUID.randomUUID());

        doNothing().when(blogPostRepository).createBlogPost(any(BlogPost.class));

        BlogPost createdBlogPost = blogPostService.createBlogPost(request.getTitle(), request.getContent(),
                request.getUserId());

        assertNotNull(createdBlogPost);
        assertEquals(request.getTitle(), createdBlogPost.getTitle());
        assertEquals(request.getContent(), createdBlogPost.getContent());
        verify(blogPostRepository, times(1)).createBlogPost(any(BlogPost.class));
    }

    @Test
    void getBlogPost_shouldReturnBlogPostIfExists() {
        UUID blogPostId = UUID.randomUUID();
        BlogPost blogPost = new BlogPost(blogPostId, "Test Blog Post", "This is a test blog post.", Instant.now(),
                Instant.now(), UUID.randomUUID());

        when(blogPostRepository.getBlogPost(blogPostId)).thenReturn(Optional.of(blogPost));

        BlogPost foundBlogPost = blogPostService.getBlogPost(blogPostId);

        assertNotNull(foundBlogPost);
        assertEquals(blogPostId, foundBlogPost.getId());
        verify(blogPostRepository, times(1)).getBlogPost(blogPostId);
    }

    @Test
    void getBlogPostsByUserId_shouldReturnBlogPostsOfUserId() {
        UUID userId = UUID.randomUUID();
        BlogPost blogPost1 = new BlogPost(UUID.randomUUID(), "Blog Post 1", "Content 1", Instant.now(),
                Instant.now(), userId);
        BlogPost blogPost2 = new BlogPost(UUID.randomUUID(), "Blog Post 2", "Content 2", Instant.now(),
                Instant.now(), userId);

        when(blogPostRepository.getBlogPostsByUserId(userId)).thenReturn(
                java.util.List.of(blogPost1, blogPost2)
        );

        java.util.List<BlogPost> blogPosts = blogPostService.getBlogPostsByUserId(userId);

        assertNotNull(blogPosts);
        assertEquals(2, blogPosts.size());
        assertEquals(userId, blogPosts.get(0).getUserId());
        assertEquals(userId, blogPosts.get(1).getUserId());
        verify(blogPostRepository, times(1)).getBlogPostsByUserId(userId);
    }

    @Test
    void updateBlogPost_shouldUpdateAndReturnUpdatedBlogPost() {
        UUID blogPostId = UUID.randomUUID();
        BlogPost existingBlogPost = new BlogPost(blogPostId, "Old Title", "Old Content", Instant.now(), Instant.now()
                , UUID.randomUUID());
        UpdateBlogPostRequest request = new UpdateBlogPostRequest();
        request.setTitle("New Title");
        request.setContent("New Content");

        when(blogPostRepository.getBlogPost(blogPostId)).thenReturn(Optional.of(existingBlogPost));
        doNothing().when(blogPostRepository).updateBlogPost(any(BlogPost.class));

        BlogPost updatedBlogPost = blogPostService.updateBlogPost(blogPostId, request.getTitle(), request.getContent());

        assertNotNull(updatedBlogPost);
        assertEquals(request.getTitle(), updatedBlogPost.getTitle());
        assertEquals(request.getContent(), updatedBlogPost.getContent());
        verify(blogPostRepository, times(1)).updateBlogPost(any(BlogPost.class));
    }

    @Test
    void deleteBlogPost_shouldDeleteBlogPostIfExists() {
        UUID blogPostId = UUID.randomUUID();

        doNothing().when(blogPostRepository).deleteBlogPost(blogPostId);

        blogPostService.deleteBlogPost(blogPostId);

        verify(blogPostRepository, times(1)).deleteBlogPost(blogPostId);
    }

    @Test
    void findPaginated_shouldReturnPageWithDefaults() {
        int page = 0;
        int size = 20;
        UUID userId = null;
        List<BlogPost> posts = List.of(new BlogPost(UUID.randomUUID(), "Post 1", "Content 1", Instant.now(), Instant.now(), UUID.randomUUID()));
        when(blogPostRepository.getPaginatedBlogPosts(eq(size), eq(page * size), eq(userId))).thenReturn(posts);
        when(blogPostRepository.countBlogPosts(eq(userId))).thenReturn(1L);

        Page<BlogPost> result = blogPostService.findPaginated(page, size, userId);

        assertEquals(posts, result.getContent());
        assertEquals(page, result.getPage());
        assertEquals(size, result.getSize());
        assertEquals(1L, result.getTotalElements());
        assertEquals(1, result.getTotalPages());
        assertTrue(result.isLast());
        verify(blogPostRepository, times(1)).getPaginatedBlogPosts(eq(size), eq(0), eq(userId));
        verify(blogPostRepository, times(1)).countBlogPosts(eq(userId));
    }

    @Test
    void findPaginated_shouldThrowForInvalidSize() {
        assertThrows(IllegalArgumentException.class, () -> blogPostService.findPaginated(0, 0, null));
        assertThrows(IllegalArgumentException.class, () -> blogPostService.findPaginated(0, 101, null));
        verifyNoInteractions(blogPostRepository);  // No calls if validation fails
    }

    @Test
    void findPaginated_shouldThrowForInvalidPage() {
        assertThrows(IllegalArgumentException.class, () -> blogPostService.findPaginated(-1, 20, null));
        verifyNoInteractions(blogPostRepository);
    }

    @Test
    void findPaginated_shouldReturnFilteredPageWithUserId() {
        int page = 0;
        int size = 20;
        UUID userId = UUID.randomUUID();
        List<BlogPost> posts = List.of(new BlogPost(UUID.randomUUID(), "Post 1", "Content 1", Instant.now(), Instant.now(), userId));
        when(blogPostRepository.getPaginatedBlogPosts(eq(size), eq(page * size), eq(userId))).thenReturn(posts);
        when(blogPostRepository.countBlogPosts(eq(userId))).thenReturn(1L);

        Page<BlogPost> result = blogPostService.findPaginated(page, size, userId);

        assertEquals(posts, result.getContent());
        assertEquals(1L, result.getTotalElements());
        assertTrue(result.isLast());
        verify(blogPostRepository, times(1)).getPaginatedBlogPosts(eq(size), eq(0), eq(userId));
        verify(blogPostRepository, times(1)).countBlogPosts(eq(userId));
    }
}
