package com.dehold.contentmanager.validation.model;

import com.dehold.contentmanager.content.Content;
import com.dehold.contentmanager.content.blogpost.model.BlogPost;
import com.dehold.contentmanager.content.customersupport.model.SupportRequest;

import java.util.Map;

public class ContentTypeRegistry {
    public static final Map<String, Class<? extends Content>> CONTENT_TYPES = Map.of(
            "blogpost", BlogPost.class,
            "supportrequest", SupportRequest.class
    );
}
