package com.dehold.contentmanager.content.blogpost.service;

import com.dehold.contentmanager.content.blogpost.model.BlogPost;
import com.dehold.contentmanager.content.blogpost.model.Comment;
import com.dehold.contentmanager.content.blogpost.repository.BlogPostRepository;
import com.dehold.contentmanager.content.blogpost.web.dto.CreateBlogPostRequest;
import com.dehold.contentmanager.content.blogpost.web.dto.UpdateBlogPostRequest;
import com.dehold.contentmanager.content.blogpost.model.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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

        // capture the BlogPost passed to repository to ensure service constructs it correctly
        ArgumentCaptor<BlogPost> captor = ArgumentCaptor.forClass(BlogPost.class);
        doNothing().when(blogPostRepository).createBlogPost(captor.capture());

        BlogPost createdBlogPost = blogPostService.createBlogPost(request.getTitle(), request.getContent(),
                request.getUserId(), null);

        assertNotNull(createdBlogPost);
        assertEquals(request.getTitle(), createdBlogPost.getTitle());
        assertEquals(request.getContent(), createdBlogPost.getContent());

        // verify repository called once and inspect the passed BlogPost object
        verify(blogPostRepository, times(1)).createBlogPost(any(BlogPost.class));
        BlogPost passed = captor.getValue();
        assertNotNull(passed.getId(), "Service must assign id before persisting");
        assertEquals(request.getTitle(), passed.getTitle());
        assertEquals(request.getContent(), passed.getContent());
        assertEquals(userId, passed.getUserId());
        assertNotNull(passed.getCreatedAt());
        assertNotNull(passed.getUpdatedAt());
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

    @Test
    void createBlogPost_withComments_shouldPassCorrectBlogPostToRepository() {
        // Arrange
        UUID userId = UUID.randomUUID();
        String title = "Post with comments";
        String content = "Body text";

        Comment comment = new Comment(null, userId, "Nice post!", null, null);

        ArgumentCaptor<BlogPost> captor = ArgumentCaptor.forClass(BlogPost.class);
        doNothing().when(blogPostRepository).createBlogPost(captor.capture());

        // Act
        BlogPost created = blogPostService.createBlogPost(title, content, userId, List.of(comment));

        // Assert returned object basic sanity
        assertNotNull(created);
        assertEquals(title, created.getTitle());
        assertEquals(content, created.getContent());

        // Assert repository invocation and inspect passed value
        verify(blogPostRepository, times(1)).createBlogPost(any(BlogPost.class));
        BlogPost passed = captor.getValue();
        assertNotNull(passed.getId(), "Service should assign id before persisting");
        assertEquals(title, passed.getTitle());
        assertEquals(content, passed.getContent());
        assertEquals(userId, passed.getUserId());
        assertNotNull(passed.getCreatedAt());
        assertNotNull(passed.getUpdatedAt());

        // Comments assertions
        assertNotNull(passed.getComments());
        assertEquals(1, passed.getComments().size());
        Comment passedComment = passed.getComments().get(0);
        assertEquals("Nice post!", passedComment.getText());
        assertEquals(userId, passedComment.getUserId());
    }

    @Test
    void updateBlogPost_withComments_shouldPassUpdatedBlogPostToRepository() {
        // Arrange
        UUID postId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Comment comment = new Comment(null, userId, "Updated comment", null, null);

        BlogPost existing = new BlogPost(postId, "Old", "OldBody", Instant.now(), Instant.now(), userId, List.of(comment));
        when(blogPostRepository.getBlogPost(postId)).thenReturn(Optional.of(existing));


        ArgumentCaptor<BlogPost> captor = ArgumentCaptor.forClass(BlogPost.class);
        doNothing().when(blogPostRepository).updateBlogPost(captor.capture());

        // Act
        BlogPost updated = blogPostService.updateBlogPost(postId, "New Title", "New Body");

        // Assert
        assertNotNull(updated);
        assertEquals("New Title", updated.getTitle());
        assertEquals("New Body", updated.getContent());

        verify(blogPostRepository, times(1)).updateBlogPost(any(BlogPost.class));
        BlogPost passed = captor.getValue();
        assertEquals(postId, passed.getId());
        assertEquals("New Title", passed.getTitle());
        assertEquals("New Body", passed.getContent());
        assertNotNull(passed.getUpdatedAt());

        assertNotNull(passed.getComments());
        assertEquals(1, passed.getComments().size());
        assertEquals("Updated comment", passed.getComments().get(0).getText());
    }

    @Test
    void getBlogPostById_shouldReturnBlogPostWithComments_andVerifyRepositoryCall() {
        // Arrange
        UUID postId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Comment c = new Comment(UUID.randomUUID(), userId, "Loaded comment", Instant.now(), Instant.now());
        BlogPost post = new BlogPost(postId, "Loaded", "Loaded body", Instant.now(), Instant.now(), userId);
        post.setComments(List.of(c));

        when(blogPostRepository.getBlogPost(postId)).thenReturn(Optional.of(post));

        // Act
        BlogPost bp = blogPostService.getBlogPost(postId);

        // Assert
        assertEquals(postId, bp.getId());
        assertNotNull(bp.getComments());
        assertEquals(1, bp.getComments().size());
        assertEquals("Loaded comment", bp.getComments().get(0).getText());
    }
}
