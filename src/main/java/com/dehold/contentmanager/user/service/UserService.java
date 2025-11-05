package com.dehold.contentmanager.user.service;

import com.dehold.contentmanager.user.model.User;
import com.dehold.contentmanager.user.web.dto.CreateUserRequest;
import com.dehold.contentmanager.user.web.dto.UpdateUserRequest;
import com.dehold.contentmanager.validation.model.ValidationPipelineModel;

import java.util.List;
import java.util.UUID;

public interface UserService {

    User createUser(CreateUserRequest dto);

    User getUser(UUID id);

    User updateUser(UUID id, UpdateUserRequest dto);

    void deleteUser(UUID id);

    List<ValidationPipelineModel> getValidationPipelineByUserIdAndContentType(UUID userId, String contentType);
}
