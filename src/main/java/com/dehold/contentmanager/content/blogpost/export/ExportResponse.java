package com.dehold.contentmanager.content.blogpost.export;

import com.dehold.contentmanager.content.blogpost.model.BlogPost;
import com.dehold.contentmanager.content.customersupport.model.SupportRequest;
import com.dehold.contentmanager.content.customersupport.model.SupportResponse;

import java.util.List;

public class ExportResponse {
    private List<BlogPost> blogPosts;
    private List<SupportRequest> supportRequests;
    private List<SupportResponse> supportResponses;

    public ExportResponse() {}

    public ExportResponse(List<BlogPost> blogPosts, List<SupportRequest> supportRequests, List<SupportResponse> supportResponses) {
        this.blogPosts = blogPosts;
        this.supportRequests = supportRequests;
        this.supportResponses = supportResponses;
    }

    public List<BlogPost> getBlogPosts() {
        return blogPosts;
    }

    public void setBlogPosts(List<BlogPost> blogPosts) {
        this.blogPosts = blogPosts;
    }

    public List<SupportRequest> getSupportRequests() {
        return supportRequests;
    }

    public void setSupportRequests(List<SupportRequest> supportRequests) {
        this.supportRequests = supportRequests;
    }

    public List<SupportResponse> getSupportResponses() {
        return supportResponses;
    }

    public void setSupportResponses(List<SupportResponse> supportResponses) {
        this.supportResponses = supportResponses;
    }
}

