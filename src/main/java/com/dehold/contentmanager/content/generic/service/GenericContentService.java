package com.dehold.contentmanager.content.generic.service;

import com.dehold.contentmanager.content.generic.model.GenericContentModel;

import java.util.UUID;

public interface GenericContentService {
    GenericContentModel getById(UUID id);
    GenericContentModel save(GenericContentModel content);
    void deleteById(UUID id);
    boolean existsById(UUID id);
}
