package com.dehold.contentmanager.content.blogpost.export;


import com.dehold.contentmanager.content.blogpost.model.BlogPost;
import com.dehold.contentmanager.content.blogpost.repository.BlogPostRepository;
import com.dehold.contentmanager.content.customersupport.model.SupportRequest;
import com.dehold.contentmanager.content.customersupport.model.SupportResponse;
import com.dehold.contentmanager.content.customersupport.repository.SupportRequestRepository;
import com.dehold.contentmanager.content.customersupport.repository.SupportResponseRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class ExportService {

    private final BlogPostRepository blogPostRepository;
    private final SupportRequestRepository supportRequestRepository;
    private final SupportResponseRepository supportResponseRepository;

    public ExportService(BlogPostRepository blogPostRepository,
                         SupportRequestRepository supportRequestRepository,
                         SupportResponseRepository supportResponseRepository) {
        this.blogPostRepository = blogPostRepository;
        this.supportRequestRepository = supportRequestRepository;
        this.supportResponseRepository = supportResponseRepository;
    }

    public ExportResponse exportAllForUser(UUID userId) {
        List<BlogPost> blogPosts = blogPostRepository.getBlogPostsByUserId(userId);
        List<SupportRequest> supportRequests = supportRequestRepository.findByUserId(userId);
        List<SupportResponse> supportResponses = supportResponseRepository.getAllSupportResponsesByUserId(userId);
        return new ExportResponse(blogPosts, supportRequests, supportResponses);
    }

    public ExportResponse exportForUserByType(UUID userId, ContentExportType contentType) {
        return exportAllForUser(userId);
    }

    public ExportResponse exportByContentIdsAndType(UUID contentId, ContentExportType contentType) {
        if (contentType == null) {
            return exportAllForUser(contentId);
        }

        switch (contentType) {
            case BLOGPOST:
                Optional<BlogPost> blogPosts = blogPostRepository.getBlogPost(contentId);
                return new ExportResponse(blogPosts.map(List::of).orElse(List.of()), List.of(), List.of());
            case SUPPORT_REQUEST:
                Optional<SupportRequest> req = supportRequestRepository.getById(contentId);
                return new ExportResponse(List.of(), req.map(List::of).orElse(List.of()), List.of());
            case SUPPORT_RESPONSE:
                Optional<SupportResponse> resp = supportResponseRepository.getById(contentId);
                return new ExportResponse(List.of(), List.of(), resp.map(List::of).orElse(List.of()));
            default:
                // fallback to all
                return exportAllForUser(contentId);
        }
    }

}

