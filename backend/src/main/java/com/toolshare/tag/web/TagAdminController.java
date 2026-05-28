package com.toolshare.tag.web;

import com.toolshare.model.ApiResponse;
import com.toolshare.security.CurrentUserHolder;
import com.toolshare.security.RequireRole;
import com.toolshare.tag.service.TagManagementService;
import com.toolshare.user.repository.RoleRepository;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/tags")
@RequireRole(RoleRepository.ADMIN)
public class TagAdminController {

    private final TagManagementService tagManagementService;

    public TagAdminController(TagManagementService tagManagementService) {
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

    @PutMapping("/{tagId}")
    public ApiResponse<TagResponse> updateTag(@PathVariable Long tagId,
                                              @Valid @RequestBody TagUpsertRequest request) {
        return ApiResponse.ok(tagManagementService.updateTag(tagId, request, CurrentUserHolder.get()));
    }

    @DeleteMapping("/{tagId}")
    public ApiResponse<Void> deleteTag(@PathVariable Long tagId) {
        tagManagementService.deleteTag(tagId, CurrentUserHolder.get());
        return ApiResponse.ok(null);
    }
}
