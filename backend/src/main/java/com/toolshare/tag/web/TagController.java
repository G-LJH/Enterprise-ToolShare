package com.toolshare.tag.web;

import com.toolshare.model.ApiResponse;
import com.toolshare.security.CurrentUserHolder;
import com.toolshare.security.RequireAuthenticated;
import com.toolshare.tag.service.TagManagementService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tags")
@RequireAuthenticated
public class TagController {

    private final TagManagementService tagManagementService;

    public TagController(TagManagementService tagManagementService) {
        this.tagManagementService = tagManagementService;
    }

    @GetMapping
    public ApiResponse<List<TagResponse>> listTags() {
        return ApiResponse.ok(tagManagementService.listTags());
    }

    @PostMapping
    public ApiResponse<TagResponse> createTag(@Valid @RequestBody TagUpsertRequest request) {
        return ApiResponse.ok(tagManagementService.createTag(request, CurrentUserHolder.get()));
    }
}
