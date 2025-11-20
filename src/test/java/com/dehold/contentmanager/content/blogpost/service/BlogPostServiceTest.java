package com.dehold.contentmanager.content.blogpost.service;

import com.dehold.contentmanager.content.blogpost.model.BlogPost;
import com.dehold.contentmanager.content.blogpost.model.Comment;
import com.dehold.contentmanager.content.blogpost.model.BlogPostHistory;
import com.dehold.contentmanager.content.blogpost.repository.BlogPostHistoryRepository;
import com.dehold.contentmanager.content.blogpost.repository.BlogPostRepository;
import com.dehold.contentmanager.content.blogpost.web.dto.CreateBlogPostRequest;
import com.dehold.contentmanager.content.blogpost.web.dto.UpdateBlogPostRequest;
import com.dehold.contentmanager.content.blogpost.model.Page;
import com.dehold.contentmanager.exception.EntityNotFoundException;
import com.dehold.contentmanager.common.exception.InvalidStateTransitionException;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class BlogPostServiceTest {

    @Mock
    private BlogPostRepository blogPostRepository;

    @InjectMocks
    private BlogPostService blogPostService;

    @Mock
    private BlogPostHistoryRepository blogPostHistoryRepository;

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
    @Test
    void updateBlogPostVersion_shouldSaveHistoryAndUpdateBlogPost() {
        UUID blogPostId = UUID.randomUUID();
        BlogPost existingBlogPost = new BlogPost(
                blogPostId, "Old Title", "Old Content", Instant.now(), Instant.now(), UUID.randomUUID()
        );

        when(blogPostRepository.getBlogPost(blogPostId)).thenReturn(Optional.of(existingBlogPost));
        when(blogPostHistoryRepository.getNextVersionNumber(blogPostId)).thenReturn(1);
        doNothing().when(blogPostHistoryRepository).saveHistory(any(BlogPost.class), anyInt());
        doNothing().when(blogPostRepository).updateBlogPost(any(BlogPost.class));

        BlogPost updated = blogPostService.updateBlogPostVersion(blogPostId, "New Title", "New Content");

        assertNotNull(updated);
        assertEquals("New Title", updated.getTitle());
        assertEquals("New Content", updated.getContent());

        verify(blogPostHistoryRepository, times(1)).saveHistory(any(BlogPost.class), eq(1));
        verify(blogPostRepository, times(1)).updateBlogPost(any(BlogPost.class));
    }

    @Test
    void updateBlogPostVersion_shouldIncrementVersionNumbersSequentially() {
        UUID blogPostId = UUID.randomUUID();
        BlogPost existingBlogPost = new BlogPost(
                blogPostId, "Title", "Content", Instant.now(), Instant.now(), UUID.randomUUID()
        );

        when(blogPostRepository.getBlogPost(blogPostId)).thenReturn(Optional.of(existingBlogPost));
        when(blogPostHistoryRepository.getNextVersionNumber(blogPostId))
                .thenReturn(1)
                .thenReturn(2)
                .thenReturn(3);
        doNothing().when(blogPostHistoryRepository).saveHistory(any(BlogPost.class), anyInt());
        doNothing().when(blogPostRepository).updateBlogPost(any(BlogPost.class));

        blogPostService.updateBlogPostVersion(blogPostId, "v1", "c1");
        blogPostService.updateBlogPostVersion(blogPostId, "v2", "c2");
        blogPostService.updateBlogPostVersion(blogPostId, "v3", "c3");

        verify(blogPostHistoryRepository, times(3)).saveHistory(any(BlogPost.class), anyInt());
        verify(blogPostRepository, times(3)).updateBlogPost(any(BlogPost.class));
    }

    @Test
    void updateBlogPostVersion_shouldThrowExceptionIfBlogPostNotFound() {
        UUID missingId = UUID.randomUUID();
        when(blogPostRepository.getBlogPost(missingId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
                blogPostService.updateBlogPostVersion(missingId, "Title", "Content")
        );

        verify(blogPostHistoryRepository, never()).saveHistory(any(), anyInt());
        verify(blogPostRepository, never()).updateBlogPost(any());
    }

    @Test
    void updateBlogPostVersion_shouldUpdateTimestampsAndKeepCreatedAtSame() {
        UUID blogPostId = UUID.randomUUID();
        Instant originalCreatedAt = Instant.now().minusSeconds(60);
        Instant originalUpdatedAt = Instant.now().minusSeconds(30);

        BlogPost existingBlogPost = new BlogPost(
                blogPostId, "Old Title", "Old Content", originalCreatedAt, originalUpdatedAt, UUID.randomUUID()
        );

        when(blogPostRepository.getBlogPost(blogPostId)).thenReturn(Optional.of(existingBlogPost));
        when(blogPostHistoryRepository.getNextVersionNumber(blogPostId)).thenReturn(1);
        doNothing().when(blogPostHistoryRepository).saveHistory(any(BlogPost.class), anyInt());
        doNothing().when(blogPostRepository).updateBlogPost(any(BlogPost.class));

        BlogPost updated = blogPostService.updateBlogPostVersion(blogPostId, "New Title", "New Content");

        assertEquals(originalCreatedAt, updated.getCreatedAt(), "CreatedAt should not change");
        assertTrue(updated.getUpdatedAt().isAfter(originalUpdatedAt), "UpdatedAt should be newer");

        verify(blogPostHistoryRepository).saveHistory(any(BlogPost.class), eq(1));
        verify(blogPostRepository).updateBlogPost(any(BlogPost.class));
    }

    @Test
    void restoreVersion_shouldRestoreVersionSuccessfully() {

        UUID postId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        BlogPostHistory history = new BlogPostHistory();
        history.setId(UUID.randomUUID());
        history.setBlogPostId(postId);
        history.setTitle("Old Title");
        history.setContent("Old Content");
        history.setVersionNumber(1);
        history.setCreatedAt(now);
        history.setUpdatedAt(now);

        when(blogPostHistoryRepository.getHistoryByBlogPostId(postId))
                .thenReturn(List.of(history));
        BlogPost existing = new BlogPost(
                postId, "Current Title", "Current Content", now, now, userId
        );
        when(blogPostRepository.getBlogPost(postId))
                .thenReturn(Optional.of(existing));
        BlogPost updated = new BlogPost(
                postId, "Old Title", "Old Content", existing.getCreatedAt(), Instant.now(), userId
        );
        when(blogPostHistoryRepository.getNextVersionNumber(postId))
                .thenReturn(2);
        doNothing().when(blogPostHistoryRepository).saveHistory(any(), eq(2));
        doNothing().when(blogPostRepository).updateBlogPost(any());
        when(blogPostRepository.getBlogPost(postId))
                .thenReturn(Optional.of(updated));
        BlogPost result = blogPostService.restoreVersion(postId, 1);
        assertNotNull(result);
        assertEquals("Old Title", result.getTitle());
        assertEquals("Old Content", result.getContent());

        verify(blogPostHistoryRepository).getHistoryByBlogPostId(postId);
        verify(blogPostHistoryRepository).saveHistory(any(), eq(2));
        verify(blogPostRepository, times(2)).getBlogPost(postId); // called twice
        verify(blogPostRepository).updateBlogPost(any());
    }

    @Test
    void getHistory_shouldReturnHistoryForBlogPost() {
        UUID postId = UUID.randomUUID();
        Instant now = Instant.now();

        BlogPostHistory h1 = new BlogPostHistory();
        h1.setId(UUID.randomUUID());
        h1.setBlogPostId(postId);
        h1.setTitle("Title v1");
        h1.setContent("Content v1");
        h1.setVersionNumber(1);
        h1.setCreatedAt(now.minusSeconds(60));
        h1.setUpdatedAt(now.minusSeconds(60));

        BlogPostHistory h2 = new BlogPostHistory();
        h2.setId(UUID.randomUUID());
        h2.setBlogPostId(postId);
        h2.setTitle("Title v2");
        h2.setContent("Content v2");
        h2.setVersionNumber(2);
        h2.setCreatedAt(now.minusSeconds(30));
        h2.setUpdatedAt(now.minusSeconds(30));

        List<BlogPostHistory> mockHistory = List.of(h2, h1); // assume ordered DESC

        when(blogPostHistoryRepository.getHistoryByBlogPostId(postId))
                .thenReturn(mockHistory);

        // ACT
        List<BlogPostHistory> result = blogPostService.getHistory(postId);

        // ASSERT
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(mockHistory, result); // exact list comparison

        assertEquals(2, result.get(0).getVersionNumber());
        assertEquals(1, result.get(1).getVersionNumber());

        verify(blogPostHistoryRepository, times(1))
                .getHistoryByBlogPostId(postId);
    }

    @Test
    void searchByTerm_shouldReturnMatchingIds_caseSensitive_positive() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        when(blogPostRepository.searchByTerm("Java"))
            .thenReturn(List.of(id1, id2));

        List<UUID> result = blogPostService.searchByTerm("Java");

        assertEquals(2, result.size());
        assertTrue(result.contains(id1));
        assertTrue(result.contains(id2));
        verify(blogPostRepository).searchByTerm("Java");
    }

    @Test
    void searchByTerm_shouldBeCaseSensitive_negative() {
       
        when(blogPostRepository.searchByTerm("java"))
            .thenReturn(List.of(UUID.randomUUID()));

        // Search for uppercase "Java" → no match
        when(blogPostRepository.searchByTerm("Java"))
            .thenReturn(List.of());

        List<UUID> result = blogPostService.searchByTerm("Java");

        assertTrue(result.isEmpty(), "Should be case-sensitive: 'Java' ≠ 'java'");
        verify(blogPostRepository).searchByTerm("Java");
    }

    @Test
    void searchByTerm_shouldReturnEmptyWhenNoMatch() {
        when(blogPostRepository.searchByTerm("NonExistentTerm"))
            .thenReturn(List.of());

        List<UUID> result = blogPostService.searchByTerm("NonExistentTerm");

        assertTrue(result.isEmpty());
        verify(blogPostRepository).searchByTerm("NonExistentTerm");
    }

    @Test
    void searchByTerm_shouldHandleNullEmptyAndWhitespaceTerm() {
        when(blogPostRepository.searchByTerm(null)).thenReturn(List.of());
        when(blogPostRepository.searchByTerm("")).thenReturn(List.of());
        when(blogPostRepository.searchByTerm("   ")).thenReturn(List.of());

        // Test Case: Null
        List<UUID> resultNull = blogPostService.searchByTerm(null);
        assertTrue(resultNull.isEmpty(), "Should return empty list for null term.");

        // Test Case: Empty String
        List<UUID> resultEmpty = blogPostService.searchByTerm("");
        assertTrue(resultEmpty.isEmpty(), "Should return empty list for empty string.");
        
        // Test Case: Whitespace Only (Trimming expectation)
        List<UUID> resultWhitespace = blogPostService.searchByTerm("   ");
        assertTrue(resultWhitespace.isEmpty(), "Should return empty list for whitespace-only string.");

        verify(blogPostRepository, times(1)).searchByTerm(null);
        verify(blogPostRepository, times(1)).searchByTerm("");
        verify(blogPostRepository, times(1)).searchByTerm("   ");
        
    }

    @Test
    void submitForReview_shouldTransitionDraftToPendingReview() {
        UUID blogPostId = UUID.randomUUID();
        BlogPost draftPost = new BlogPost(blogPostId, "Title", "Content", Instant.now(), Instant.now(), UUID.randomUUID());
        draftPost.setState(BlogPost.State.DRAFT);

        when(blogPostRepository.getBlogPost(blogPostId)).thenReturn(Optional.of(draftPost));
        doNothing().when(blogPostRepository).updateBlogPost(any(BlogPost.class));

        BlogPost updatedPost = blogPostService.submitForReview(blogPostId);

        assertEquals(BlogPost.State.PENDING_REVIEW, updatedPost.getState());
        verify(blogPostRepository).updateBlogPost(updatedPost);
    }

    @Test
    void approveBlogPost_shouldTransitionPendingReviewToApproved() {
        UUID blogPostId = UUID.randomUUID();
        BlogPost pendingPost = new BlogPost(blogPostId, "Title", "Content", Instant.now(), Instant.now(), UUID.randomUUID());
        pendingPost.setState(BlogPost.State.PENDING_REVIEW);

        when(blogPostRepository.getBlogPost(blogPostId)).thenReturn(Optional.of(pendingPost));
        doNothing().when(blogPostRepository).updateBlogPost(any(BlogPost.class));

        BlogPost updatedPost = blogPostService.approveBlogPost(blogPostId);

        assertEquals(BlogPost.State.APPROVED, updatedPost.getState());
        verify(blogPostRepository).updateBlogPost(updatedPost);
    }

    @Test
    void rejectBlogPost_shouldTransitionPendingReviewToRejected() {
        UUID blogPostId = UUID.randomUUID();
        BlogPost pendingPost = new BlogPost(blogPostId, "Title", "Content", Instant.now(), Instant.now(), UUID.randomUUID());
        pendingPost.setState(BlogPost.State.PENDING_REVIEW);

        when(blogPostRepository.getBlogPost(blogPostId)).thenReturn(Optional.of(pendingPost));
        doNothing().when(blogPostRepository).updateBlogPost(any(BlogPost.class));

        BlogPost updatedPost = blogPostService.rejectBlogPost(blogPostId);

        assertEquals(BlogPost.State.REJECTED, updatedPost.getState());
        verify(blogPostRepository).updateBlogPost(updatedPost);
    }

    @Test
    void submitForReview_shouldThrowExceptionIfNotDraft() {
        UUID blogPostId = UUID.randomUUID();
        BlogPost nonDraftPost = new BlogPost(blogPostId, "Title", "Content", Instant.now(), Instant.now(), UUID.randomUUID());
        nonDraftPost.setState(BlogPost.State.APPROVED);

        when(blogPostRepository.getBlogPost(blogPostId)).thenReturn(Optional.of(nonDraftPost));

        assertThrows(InvalidStateTransitionException.class, () -> blogPostService.submitForReview(blogPostId));
        verify(blogPostRepository, never()).updateBlogPost(any());
    }

    @Test
    void approveBlogPost_shouldThrowExceptionIfNotPendingReview() {
        UUID blogPostId = UUID.randomUUID();
        BlogPost nonPendingPost = new BlogPost(blogPostId, "Title", "Content", Instant.now(), Instant.now(), UUID.randomUUID());
        nonPendingPost.setState(BlogPost.State.DRAFT);

        when(blogPostRepository.getBlogPost(blogPostId)).thenReturn(Optional.of(nonPendingPost));

        assertThrows(InvalidStateTransitionException.class, () -> blogPostService.approveBlogPost(blogPostId));
        verify(blogPostRepository, never()).updateBlogPost(any());
    }

    @Test
    void rejectBlogPost_shouldThrowExceptionIfNotPendingReview() {
        UUID blogPostId = UUID.randomUUID();
        BlogPost nonPendingPost = new BlogPost(blogPostId, "Title", "Content", Instant.now(), Instant.now(), UUID.randomUUID());
        nonPendingPost.setState(BlogPost.State.DRAFT);

        when(blogPostRepository.getBlogPost(blogPostId)).thenReturn(Optional.of(nonPendingPost));

        assertThrows(InvalidStateTransitionException.class, () -> blogPostService.rejectBlogPost(blogPostId));
        verify(blogPostRepository, never()).updateBlogPost(any());
    }
}
