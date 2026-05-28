package com.toolshare.systemconfig.web;

import com.toolshare.model.ApiResponse;
import com.toolshare.security.CurrentUserHolder;
import com.toolshare.security.RequireRole;
import com.toolshare.systemconfig.service.SystemConfigService;
import com.toolshare.user.repository.RoleRepository;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/system-configs")
@RequireRole(RoleRepository.ADMIN)
public class AdminSystemConfigController {

    private final SystemConfigService systemConfigService;

    public AdminSystemConfigController(SystemConfigService systemConfigService) {
        this.systemConfigService = systemConfigService;
    }

    @GetMapping("/tool-review")
    public ApiResponse<ToolReviewConfigResponse> getToolReviewConfig() {
        return ApiResponse.ok(systemConfigService.getToolReviewConfig());
    }

    @PutMapping("/tool-review")
    public ApiResponse<ToolReviewConfigResponse> updateToolReviewConfig(@Valid @RequestBody ToolReviewConfigUpdateRequest request) {
        return ApiResponse.ok(systemConfigService.updateToolReviewEnabled(request.toolReviewEnabled(), CurrentUserHolder.get()));
    }
}
