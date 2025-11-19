package com.dehold.contentmanager.content.customersupport.service;

import com.dehold.contentmanager.content.customersupport.model.SupportResponse;

import java.util.List;
import java.util.UUID;

public interface SupportResponseService {
    void createSupportResponse(SupportResponse response);
    SupportResponse getSupportResponse(UUID id);
    void updateSupportResponse(SupportResponse response);
    void deleteSupportResponse(UUID id);
    List<SupportResponse> getSupportResponsesByUserId(UUID userId);
}

