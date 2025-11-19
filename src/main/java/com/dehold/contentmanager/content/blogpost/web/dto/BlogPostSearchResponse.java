package com.dehold.contentmanager.content.blogpost.web.dto;

import java.util.List;
import java.util.UUID;

public record BlogPostSearchResponse(List<UUID> blogPostIds) {}