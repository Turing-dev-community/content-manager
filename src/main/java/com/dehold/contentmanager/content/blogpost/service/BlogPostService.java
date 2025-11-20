package com.dehold.contentmanager.content.blogpost.service;

import com.dehold.contentmanager.content.blogpost.export.ExportCsvConverter;
import com.dehold.contentmanager.content.blogpost.export.ExportResponse;
import com.dehold.contentmanager.content.blogpost.export.ExportService;
import com.dehold.contentmanager.content.blogpost.export.ExportXmlConverter;
import com.dehold.contentmanager.content.blogpost.model.BlogPost;
import com.dehold.contentmanager.content.blogpost.model.Comment;
import com.dehold.contentmanager.content.blogpost.model.BlogPostHistory;
import com.dehold.contentmanager.content.blogpost.repository.BlogPostHistoryRepository;
import com.dehold.contentmanager.content.blogpost.repository.BlogPostRepository;
import com.dehold.contentmanager.exception.EntityNotFoundException;
import com.dehold.contentmanager.common.exception.InvalidStateTransitionException;
import com.dehold.contentmanager.content.blogpost.model.Page;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class BlogPostService {

    private final BlogPostRepository blogPostRepository;
    private final BlogPostHistoryRepository blogPostHistoryRepository;
    private final ObjectMapper objectMapper;
    private final ExportService exportService;


    public BlogPostService(BlogPostRepository blogPostRepository, BlogPostHistoryRepository blogPostHistoryRepository, ObjectMapper objectMapper, ExportService exportService) {
        this.blogPostRepository = blogPostRepository;
        this.blogPostHistoryRepository = blogPostHistoryRepository;
        this.objectMapper = objectMapper;
        this.exportService = exportService;
    }

    public BlogPost createBlogPost(String title, String content, UUID userId, List<Comment> comments) {
        BlogPost blogPost = new BlogPost(
                UUID.randomUUID(),
                title,
                content,
                Instant.now(),
                Instant.now(),
                userId,
                comments
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

    public ResponseEntity<byte[]> getBlogPostsByUserIdAndContentType(UUID userId, String format) throws Exception {
        ExportResponse resp = exportService.exportAllForUser(userId);

        if ("csv".equalsIgnoreCase(format)) {
            byte[] csvBytes = ExportCsvConverter.toCsvBytes(resp);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.valueOf("text/csv"));
            headers.setContentDisposition(
                    ContentDisposition.attachment()
                            .filename("export-" + userId + ".csv")
                            .build()
            );
            headers.setContentLength(csvBytes.length);
            return new ResponseEntity<>(csvBytes, headers, HttpStatus.OK);
        } else if ("xml".equalsIgnoreCase(format)) {
            byte[] xml = ExportXmlConverter.toXmlBytes(resp);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_XML);
            headers.setContentDisposition(
                    ContentDisposition.attachment()
                            .filename("export-" + userId + ".xml")
                            .build());
            headers.setContentLength(xml.length);

            return new ResponseEntity<>(xml, headers, HttpStatus.OK);
        }
        else {
            // default json
            byte[] jsonBytes = objectMapper.writeValueAsBytes(resp);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setContentDisposition(
                    ContentDisposition.attachment()
                            .filename("export-" + userId + ".json")
                            .build()
            );
            headers.setContentLength(jsonBytes.length);

            return new ResponseEntity<>(jsonBytes, headers, HttpStatus.OK);
        }
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

    public List<UUID> searchByTerm(String term) {
        return blogPostRepository.searchByTerm(term);
    }

    public BlogPost submitForReview(UUID id) {
        BlogPost blogPost = getBlogPost(id);
        if (blogPost.getState() != BlogPost.State.DRAFT) {
            throw new InvalidStateTransitionException("Only DRAFT posts can be submitted for review.");
        }
        blogPost.setState(BlogPost.State.PENDING_REVIEW);
        blogPost.setUpdatedAt(Instant.now());
        blogPostRepository.updateBlogPost(blogPost);
        return blogPost;
    }

    public BlogPost approveBlogPost(UUID id) {
        BlogPost blogPost = getBlogPost(id);
        if (blogPost.getState() != BlogPost.State.PENDING_REVIEW) {
            throw new InvalidStateTransitionException("Only PENDING_REVIEW posts can be approved.");
        }
        blogPost.setState(BlogPost.State.APPROVED);
        blogPost.setUpdatedAt(Instant.now());
        blogPostRepository.updateBlogPost(blogPost);
        return blogPost;
    }

    public BlogPost rejectBlogPost(UUID id) {
        BlogPost blogPost = getBlogPost(id);
        if (blogPost.getState() != BlogPost.State.PENDING_REVIEW) {
            throw new InvalidStateTransitionException("Only PENDING_REVIEW posts can be rejected.");
        }
        blogPost.setState(BlogPost.State.REJECTED);
        blogPost.setUpdatedAt(Instant.now());
        blogPostRepository.updateBlogPost(blogPost);
        return blogPost;
    }
}
