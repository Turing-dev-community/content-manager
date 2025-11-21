package com.dehold.contentmanager.content.blogpost.service;

import com.dehold.contentmanager.content.blogpost.export.ContentExportType;
import com.dehold.contentmanager.content.blogpost.export.ExportCsvConverter;
import com.dehold.contentmanager.content.blogpost.export.ExportResponse;
import com.dehold.contentmanager.content.blogpost.export.ExportService;
import com.dehold.contentmanager.content.blogpost.export.ExportXmlConverter;
import com.dehold.contentmanager.content.blogpost.model.BlogPost;
import com.dehold.contentmanager.content.blogpost.model.Comment;
import com.dehold.contentmanager.content.blogpost.model.BlogPostHistory;
import com.dehold.contentmanager.content.blogpost.repository.BlogPostHistoryRepository;
import com.dehold.contentmanager.content.blogpost.repository.BlogPostRepository;
import com.dehold.contentmanager.content.customersupport.model.SupportRequest;
import com.dehold.contentmanager.content.customersupport.model.SupportResponse;
import com.dehold.contentmanager.exception.EntityNotFoundException;
import com.dehold.contentmanager.content.blogpost.model.Page;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
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

    public ResponseEntity<byte[]> getBlogPostsByUserIdAndContentType(List<UUID> userIds, String format, String contentTypeParam, boolean multiUser) throws Exception {

        // parse contentType param
        ContentExportType contentType = ContentExportType.fromStringIgnoreCase(contentTypeParam);
        if (contentTypeParam != null && contentType == null) {
            String msg = "Invalid contentType: " + contentTypeParam + ". Supported: blogpost, supportrequest, supportresponse";
            return ResponseEntity.badRequest().contentType(MediaType.TEXT_PLAIN).body(msg.getBytes(StandardCharsets.UTF_8));
        }

        // Aggregate export results
        List<BlogPost> posts = new ArrayList<>();
        List<SupportRequest> reqs = new ArrayList<>();
        List<SupportResponse> resps = new ArrayList<>();

        for (UUID uid : userIds) {
            ExportResponse partial = exportService.exportForUserByType(uid, contentType);
            if (partial != null) {
                if (partial.getBlogPosts() != null) posts.addAll(partial.getBlogPosts());
                if (partial.getSupportRequests() != null) reqs.addAll(partial.getSupportRequests());
                if (partial.getSupportResponses() != null) resps.addAll(partial.getSupportResponses());
            }
        }

        ExportResponse combined = new ExportResponse(posts, reqs, resps);

        String filename;
        if (!multiUser && userIds.size() == 1) {
            // keep filename identical for single-user export
            filename = "export-" + userIds.get(0) + "." + format.toLowerCase();
        } else {
            // BULK / MULTI-USER → use bulk filename
            filename = "export-bulk-users." + format.toLowerCase();
        }

        HttpHeaders headers = new HttpHeaders();
        byte[] payload;

        if ("csv".equalsIgnoreCase(format)) {
            payload = ExportCsvConverter.toCsvBytes(combined);
            headers.setContentType(MediaType.valueOf("text/csv"));
        } else if ("xml".equalsIgnoreCase(format)) {
            payload = ExportXmlConverter.toXmlBytes(combined);
            headers.setContentType(MediaType.APPLICATION_XML);
        } else {
            // default = JSON
            payload = objectMapper.writeValueAsBytes(combined);
            headers.setContentType(MediaType.APPLICATION_JSON);
        }

        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        headers.setContentLength(payload.length);

        return new ResponseEntity<>(payload, headers, HttpStatus.OK);
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

    public void softDeleteBlogPost(UUID id) {
        // ensure post exists (including soft-deleted)
        blogPostRepository.getBlogPost(id, true)
                .orElseThrow(() -> EntityNotFoundException.of("BlogPost", id.toString()));

        blogPostRepository.softDelete(id);
    }

    public BlogPost getBlogPost(UUID id, boolean includeSoftDeleted) {
        return blogPostRepository.getBlogPost(id, includeSoftDeleted)
                .orElseThrow(() -> EntityNotFoundException.of("BlogPost", id.toString()));
    }

    public Page<BlogPost> findPaginated(int page, int size, UUID userId, boolean includeSoftDeleted) {
        if (page < 0) {
            throw new IllegalArgumentException("Page must be non-negative");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("Size must be between 1 and 100");
        }

        int offset = page * size;

        List<BlogPost> posts = blogPostRepository.getPaginatedBlogPosts(size, offset, userId, includeSoftDeleted);
        long total = blogPostRepository.countBlogPosts(userId, includeSoftDeleted);

        return new Page<>(posts, page, size, total);
    }

}
