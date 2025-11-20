package com.dehold.contentmanager.validation.model;

import com.dehold.contentmanager.content.Content;
import com.dehold.contentmanager.content.blogpost.model.BlogPost;
import com.dehold.contentmanager.content.customersupport.model.SupportRequest;
import com.dehold.contentmanager.content.customersupport.model.SupportResponse;
import com.dehold.contentmanager.content.generic.model.GenericContentModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ContentTypeRegistryTest {

    @Test
    void getContentClass_whenBlogPost_shouldReturnBlogPostClass() {
        Class<? extends Content> result = ContentTypeRegistry.getContentClass("blogpost");
        assertEquals(BlogPost.class, result);
    }

    @Test
    void getContentClass_whenSupportResponse_shouldReturnSupportResponseClass() {
        Class<? extends Content> result = ContentTypeRegistry.getContentClass("supportresponse");
        assertEquals(SupportResponse.class, result);
    }
    @Test
    void getContentClass_whenSupportRequest_shouldReturnSupportRequestClass() {
        Class<? extends Content> result = ContentTypeRegistry.getContentClass("supportrequest");
        assertEquals(SupportRequest.class, result);
    }

    @Test
    void getContentClass_whenAnyCustomContent_shouldReturnGenericContentModelClass() {
        Class<? extends Content> result = ContentTypeRegistry.getContentClass("customcontent");
        assertEquals(GenericContentModel.class, result);

        result = ContentTypeRegistry.getContentClass("othercustomcontent");
        assertEquals(GenericContentModel.class, result);
    }
}