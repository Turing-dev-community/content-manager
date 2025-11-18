package com.dehold.contentmanager.content.blogpost.export;


import com.dehold.contentmanager.content.blogpost.model.BlogPost;
import com.dehold.contentmanager.content.blogpost.repository.BlogPostRepository;
import com.dehold.contentmanager.content.customersupport.model.SupportRequest;
import com.dehold.contentmanager.content.customersupport.model.SupportResponse;
import com.dehold.contentmanager.content.customersupport.repository.SupportRequestRepository;
import com.dehold.contentmanager.content.customersupport.repository.SupportResponseRepository;
import org.springframework.stereotype.Service;

import java.util.List;
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
}

