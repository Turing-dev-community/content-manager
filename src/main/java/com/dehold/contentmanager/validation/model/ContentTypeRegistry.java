package com.dehold.contentmanager.validation.model;

import com.dehold.contentmanager.content.Content;
import com.dehold.contentmanager.content.blogpost.model.BlogPost;
import com.dehold.contentmanager.content.customersupport.model.SupportRequest;
import com.dehold.contentmanager.content.customersupport.model.SupportResponse;
import com.dehold.contentmanager.content.generic.model.GenericContentModel;

import java.util.HashMap;
import java.util.Map;

public class ContentTypeRegistry {
    private static final Map<String, Class<? extends Content>> FIXED_TYPES = Map.of(
            "blogpost", BlogPost.class,
            "supportresponse", SupportResponse.class,
            "supportrequest", SupportRequest.class
    );

    private static final Map<String, Class<? extends Content>> dynamicTypes = new HashMap<>();

    public static Class<? extends Content> getContentClass(String contentType) {
        String normalizedType = contentType.toLowerCase();

        if (FIXED_TYPES.containsKey(normalizedType)) {
            return FIXED_TYPES.get(normalizedType);
        }

        if (dynamicTypes.containsKey(normalizedType)) {
            return dynamicTypes.get(normalizedType);
        }

        dynamicTypes.put(normalizedType, GenericContentModel.class);
        return GenericContentModel.class;
    }

    public static Map<String, Class<? extends Content>> getAllContentTypes() {
        Map<String, Class<? extends Content>> all = new HashMap<>(FIXED_TYPES);
        all.putAll(dynamicTypes);
        return all;
    }
}
