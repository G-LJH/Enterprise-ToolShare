package com.toolshare.tool.web;

import com.toolshare.model.ApiResponse;
import com.toolshare.security.CurrentUserHolder;
import com.toolshare.security.RequireRole;
import com.toolshare.tool.service.ToolManagementService;
import com.toolshare.user.repository.RoleRepository;
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
@RequestMapping("/api/admin/tools")
@RequireRole(RoleRepository.ADMIN)
public class AdminToolController {

    private final ToolManagementService toolManagementService;

    public AdminToolController(ToolManagementService toolManagementService) {
        this.toolManagementService = toolManagementService;
    }

    @GetMapping
    public ApiResponse<List<ToolListItemResponse>> listTools(@RequestParam(required = false) String keyword,
                                                             @RequestParam(required = false) String status,
                                                             @RequestParam(required = false) List<Long> tagIds,
                                                             @RequestParam(required = false) String sortBy,
                                                             @RequestParam(required = false) String sortOrder) {
        return ApiResponse.ok(toolManagementService.listAdminTools(keyword, status, tagIds, sortBy, sortOrder));
    }

    @GetMapping("/{toolId}")
    public ApiResponse<ToolDetailResponse> getTool(@PathVariable Long toolId) {
        return ApiResponse.ok(toolManagementService.getAdminToolDetail(toolId));
    }

    @PostMapping
    public ApiResponse<ToolDetailResponse> createTool(@Valid @RequestBody ToolUpsertRequest request) {
        return ApiResponse.ok(toolManagementService.createToolByAdmin(request, CurrentUserHolder.get()));
    }

    @PutMapping("/{toolId}")
    public ApiResponse<ToolDetailResponse> updateTool(@PathVariable Long toolId,
                                                      @Valid @RequestBody ToolUpsertRequest request) {
        return ApiResponse.ok(toolManagementService.updateToolByAdmin(toolId, request, CurrentUserHolder.get()));
    }

    @DeleteMapping("/{toolId}")
    public ApiResponse<Void> deleteTool(@PathVariable Long toolId) {
        toolManagementService.deleteTool(toolId, CurrentUserHolder.get());
        return ApiResponse.ok(null);
    }
}
