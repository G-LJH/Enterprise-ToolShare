package com.toolshare.tool.web;

import com.toolshare.model.ApiResponse;
import com.toolshare.security.CurrentUserHolder;
import com.toolshare.security.RequireAuthenticated;
import com.toolshare.tool.service.ToolManagementService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tools")
@RequireAuthenticated
public class ToolController {

    private final ToolManagementService toolManagementService;

    public ToolController(ToolManagementService toolManagementService) {
        this.toolManagementService = toolManagementService;
    }

    @GetMapping
    public ApiResponse<List<ToolListItemResponse>> listTools(@RequestParam(required = false) String keyword,
                                                             @RequestParam(required = false, defaultValue = "false") boolean mine,
                                                             @RequestParam(required = false) String status,
                                                             @RequestParam(required = false) List<Long> tagIds,
                                                             @RequestParam(required = false) String sortBy,
                                                             @RequestParam(required = false) String sortOrder) {
        return ApiResponse.ok(toolManagementService.listUserVisibleTools(keyword, mine, status, tagIds, sortBy, sortOrder, CurrentUserHolder.get()));
    }

    @GetMapping("/{toolId}")
    public ApiResponse<ToolDetailResponse> getTool(@PathVariable Long toolId) {
        return ApiResponse.ok(toolManagementService.getToolDetailForUser(toolId, CurrentUserHolder.get()));
    }

    @GetMapping("/favorites")
    public ApiResponse<List<ToolListItemResponse>> listFavorites() {
        return ApiResponse.ok(toolManagementService.listFavoriteTools(CurrentUserHolder.get()));
    }

    @PostMapping("/submissions")
    public ApiResponse<ToolDetailResponse> submitTool(@Valid @RequestBody ToolUpsertRequest request) {
        return ApiResponse.ok(toolManagementService.submitTool(request, CurrentUserHolder.get()));
    }

    @PutMapping("/{toolId}/resubmission")
    public ApiResponse<ToolDetailResponse> resubmitTool(@PathVariable Long toolId,
                                                        @Valid @RequestBody ToolUpsertRequest request) {
        return ApiResponse.ok(toolManagementService.resubmitRejectedTool(toolId, request, CurrentUserHolder.get()));
    }

    @DeleteMapping("/{toolId}")
    public ApiResponse<Void> deleteTool(@PathVariable Long toolId) {
        toolManagementService.deleteRejectedToolByOwner(toolId, CurrentUserHolder.get());
        return ApiResponse.ok(null);
    }
}
