package com.dehold.contentmanager.content.blogpost.service;

import com.dehold.contentmanager.content.blogpost.model.BlogPost;
import com.dehold.contentmanager.content.blogpost.model.BlogPostHistory;
import com.dehold.contentmanager.content.blogpost.repository.BlogPostHistoryRepository;
import com.dehold.contentmanager.content.blogpost.repository.BlogPostRepository;
import com.dehold.contentmanager.exception.EntityNotFoundException;
import com.dehold.contentmanager.content.blogpost.model.Page;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class BlogPostService {

    private final BlogPostRepository blogPostRepository;
    private final BlogPostHistoryRepository blogPostHistoryRepository;

    public BlogPostService(BlogPostRepository blogPostRepository, BlogPostHistoryRepository blogPostHistoryRepository) {
        this.blogPostRepository = blogPostRepository;
        this.blogPostHistoryRepository = blogPostHistoryRepository;
    }

    public BlogPost createBlogPost(String title, String content, UUID userId) {
        BlogPost blogPost = new BlogPost(
                UUID.randomUUID(),
                title,
                content,
                Instant.now(),
                Instant.now(),
                userId
        );
        blogPostRepository.createBlogPost(blogPost);
        return blogPost;
    }

    public BlogPost getBlogPost(UUID id) {
        return blogPostRepository.getBlogPost(id)
                .orElseThrow(() -> EntityNotFoundException.of("BlogPost", id.toString()));
    }

    public List<BlogPost> getAllBlogPosts() {
        return blogPostRepository.getAllBlogPosts();
    }

    public BlogPost updateBlogPost(UUID id, String title, String content) {
        BlogPost blogPost = getBlogPost(id); // This will now throw EntityNotFoundException if not found
        blogPost.setTitle(title);
        blogPost.setContent(content);
        blogPost.setUpdatedAt(Instant.now());
        blogPostRepository.updateBlogPost(blogPost);
        return blogPost;
    }

    public void deleteBlogPost(UUID id) {
        blogPostRepository.deleteBlogPost(id);
    }

    public BlogPost updateBlogPostVersion(UUID id, String title, String content) {
        // Step 1: Retrieve current post (throws EntityNotFoundException if not found)
        BlogPost existingPost = getBlogPost(id);

        // Step 2: Determine next version number
        int nextVersion = blogPostHistoryRepository.getNextVersionNumber(id);

        // Step 3: Save current post state into history table
        blogPostHistoryRepository.saveHistory(existingPost, nextVersion);

        // Step 4: Update main blog post with new data
        existingPost.setTitle(title);
        existingPost.setContent(content);
        existingPost.setUpdatedAt(Instant.now());
        blogPostRepository.updateBlogPost(existingPost);

        // Step 5: Return updated entity
        return existingPost;
    }


    public List<BlogPost> getBlogPostsByUserId(UUID userId) {
        return blogPostRepository.getBlogPostsByUserId(userId);
    }

    public Page<BlogPost> findPaginated(int page, int size, UUID userId) {
        if (page < 0) {
            throw new IllegalArgumentException("Page must be non-negative");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("Size must be between 1 and 100");
        }
        int offset = page * size;
        List<BlogPost> posts = blogPostRepository.getPaginatedBlogPosts(size, offset, userId);
        long total = blogPostRepository.countBlogPosts(userId);
        return new Page<>(posts, page, size, total);
    }

    public List<BlogPostHistory> getHistory(UUID blogPostId) {
        return blogPostHistoryRepository.getHistoryByBlogPostId(blogPostId);
    }

    public BlogPost restoreVersion(UUID blogPostId, int versionNumber) {
        BlogPostHistory version = blogPostHistoryRepository.getHistoryByBlogPostId(blogPostId)
                .stream()
                .filter(v -> v.getVersionNumber() == versionNumber)
                .findFirst()
                .orElseThrow(() -> EntityNotFoundException.of("BlogPostVersion", versionNumber + ""));
        updateBlogPostVersion(blogPostId, version.getTitle(), version.getContent());
        return getBlogPost(blogPostId);
    }
}
