package com.dehold.contentmanager.validation.model;

import com.dehold.contentmanager.content.Content;
import com.dehold.contentmanager.content.blogpost.model.BlogPost;
import com.dehold.contentmanager.content.customersupport.model.SupportRequest;
import com.dehold.contentmanager.content.customersupport.model.SupportResponse;
import com.dehold.contentmanager.content.generic.model.GenericContentModel;

import java.util.Map;

public class ContentTypeRegistry {
    private static final Map<String, Class<? extends Content>> CONTENT_TYPES = Map.of(
            "blogpost", BlogPost.class,
            "supportresponse", SupportResponse.class,
            "supportrequest", SupportResponse.class
    );

    public static Class<? extends Content> getContentClass(String contentType) {
        String normalizedType = contentType;
        return CONTENT_TYPES.getOrDefault(normalizedType, GenericContentModel.class);
    }
}
