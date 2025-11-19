package com.dehold.contentmanager.content.generic.service;

import com.dehold.contentmanager.content.generic.model.GenericContentModel;

import java.util.UUID;

public interface GenericContentService {
    GenericContentModel getById(UUID id);
    GenericContentModel create(GenericContentModel content);
    GenericContentModel update(GenericContentModel content);
    void deleteById(UUID id);
    boolean existsById(UUID id);
}
