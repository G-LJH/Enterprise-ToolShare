package com.toolshare.workflow.web;

import com.toolshare.model.ApiResponse;
import com.toolshare.review.web.ReviewDecisionRequest;
import com.toolshare.security.CurrentUserHolder;
import com.toolshare.security.RequireAdmin;
import com.toolshare.workflow.service.WorkflowReviewService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/workflow-reviews")
@RequireAdmin
public class WorkflowReviewAdminController {

    private final WorkflowReviewService workflowReviewService;

    public WorkflowReviewAdminController(WorkflowReviewService workflowReviewService) {
        this.workflowReviewService = workflowReviewService;
    }

    @GetMapping
    public ApiResponse<List<WorkflowReviewResponse>> listReviews(@RequestParam(required = false, defaultValue = "PENDING") String status) {
        return ApiResponse.ok(workflowReviewService.listSubmissions(status));
    }

    @PostMapping("/{submissionId}/approve")
    public ApiResponse<WorkflowReviewResponse> approve(@PathVariable Long submissionId,
                                                       @Valid @RequestBody(required = false) ReviewDecisionRequest request) {
        return ApiResponse.ok(workflowReviewService.approveSubmission(submissionId, request, CurrentUserHolder.get()));
    }

    @PostMapping("/{submissionId}/reject")
    public ApiResponse<WorkflowReviewResponse> reject(@PathVariable Long submissionId,
                                                      @Valid @RequestBody ReviewDecisionRequest request) {
        return ApiResponse.ok(workflowReviewService.rejectSubmission(submissionId, request, CurrentUserHolder.get()));
    }
}
