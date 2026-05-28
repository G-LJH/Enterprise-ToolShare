package com.toolshare.review.web;

import com.toolshare.model.ApiResponse;
import com.toolshare.review.service.ToolReviewService;
import com.toolshare.security.CurrentUserHolder;
import com.toolshare.security.RequireRole;
import com.toolshare.systemconfig.service.SystemConfigService;
import com.toolshare.systemconfig.web.ToolReviewConfigResponse;
import com.toolshare.systemconfig.web.ToolReviewConfigUpdateRequest;
import com.toolshare.user.repository.RoleRepository;
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
@RequestMapping("/api/admin/reviews")
@RequireRole({RoleRepository.ADMIN, RoleRepository.REVIEWER})
public class AdminToolReviewController {

    private final ToolReviewService toolReviewService;
    private final SystemConfigService systemConfigService;

    public AdminToolReviewController(ToolReviewService toolReviewService,
                                     SystemConfigService systemConfigService) {
        this.toolReviewService = toolReviewService;
        this.systemConfigService = systemConfigService;
    }

    @GetMapping
    public ApiResponse<List<ToolReviewResponse>> listReviews(@RequestParam(required = false) String status) {
        return ApiResponse.ok(toolReviewService.listSubmissions(status));
    }

    @GetMapping("/config/tool-review")
    public ApiResponse<ToolReviewConfigResponse> getToolReviewConfig() {
        return ApiResponse.ok(systemConfigService.getToolReviewConfig());
    }

    @PutMapping("/config/tool-review")
    @RequireRole(RoleRepository.ADMIN)
    public ApiResponse<ToolReviewConfigResponse> updateToolReviewConfig(@RequestBody ToolReviewConfigUpdateRequest request) {
        return ApiResponse.ok(systemConfigService.updateToolReviewEnabled(request.toolReviewEnabled(), CurrentUserHolder.get()));
    }

    @PostMapping("/{submissionId}/approve")
    public ApiResponse<ToolReviewResponse> approveReview(@PathVariable Long submissionId,
                                                         @RequestBody(required = false) ReviewDecisionRequest request) {
        return ApiResponse.ok(toolReviewService.approveSubmission(submissionId, request, CurrentUserHolder.get()));
    }

    @PostMapping("/{submissionId}/reject")
    public ApiResponse<ToolReviewResponse> rejectReview(@PathVariable Long submissionId,
                                                        @RequestBody(required = false) ReviewDecisionRequest request) {
        return ApiResponse.ok(toolReviewService.rejectSubmission(submissionId, request, CurrentUserHolder.get()));
    }
}
