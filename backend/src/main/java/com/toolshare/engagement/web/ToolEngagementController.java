package com.toolshare.engagement.web;

import com.toolshare.engagement.service.ToolEngagementService;
import com.toolshare.model.ApiResponse;
import com.toolshare.security.CurrentUserHolder;
import com.toolshare.security.RequireAuthenticated;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tools")
@RequireAuthenticated
public class ToolEngagementController {

    private final ToolEngagementService toolEngagementService;

    public ToolEngagementController(ToolEngagementService toolEngagementService) {
        this.toolEngagementService = toolEngagementService;
    }

    @PostMapping("/{toolId}/stars")
    public ApiResponse<Void> starTool(@PathVariable Long toolId) {
        toolEngagementService.starTool(toolId, CurrentUserHolder.get());
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{toolId}/stars")
    public ApiResponse<Void> unstarTool(@PathVariable Long toolId) {
        toolEngagementService.unstarTool(toolId, CurrentUserHolder.get());
        return ApiResponse.ok(null);
    }

    @PostMapping("/{toolId}/favorites")
    public ApiResponse<Void> favoriteTool(@PathVariable Long toolId) {
        toolEngagementService.favoriteTool(toolId, CurrentUserHolder.get());
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{toolId}/favorites")
    public ApiResponse<Void> unfavoriteTool(@PathVariable Long toolId) {
        toolEngagementService.unfavoriteTool(toolId, CurrentUserHolder.get());
        return ApiResponse.ok(null);
    }

    @GetMapping("/{toolId}/comments")
    public ApiResponse<List<ToolCommentResponse>> listComments(@PathVariable Long toolId) {
        return ApiResponse.ok(toolEngagementService.listComments(toolId, CurrentUserHolder.get()));
    }

    @PostMapping("/{toolId}/comments")
    public ApiResponse<ToolCommentResponse> createComment(@PathVariable Long toolId,
                                                          @Valid @RequestBody CommentCreateRequest request) {
        return ApiResponse.ok(toolEngagementService.createComment(toolId, request, CurrentUserHolder.get()));
    }

    @DeleteMapping("/{toolId}/comments/{commentId}")
    public ApiResponse<Void> deleteComment(@PathVariable Long toolId, @PathVariable Long commentId) {
        toolEngagementService.deleteComment(toolId, commentId, CurrentUserHolder.get());
        return ApiResponse.ok(null);
    }
}
