package com.toolshare.workflow.web;

import com.toolshare.model.ApiResponse;
import com.toolshare.security.CurrentUserHolder;
import com.toolshare.security.RequireAuthenticated;
import com.toolshare.workflow.service.WorkflowEngagementService;
import com.toolshare.workflow.service.WorkflowManagementService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/workflows")
@RequireAuthenticated
public class WorkflowEngagementController {

    private final WorkflowEngagementService workflowEngagementService;
    private final WorkflowManagementService workflowManagementService;

    public WorkflowEngagementController(WorkflowEngagementService workflowEngagementService,
                                        WorkflowManagementService workflowManagementService) {
        this.workflowEngagementService = workflowEngagementService;
        this.workflowManagementService = workflowManagementService;
    }

    @PostMapping("/{workflowId}/stars")
    public ApiResponse<Void> starWorkflow(@PathVariable Long workflowId) {
        workflowEngagementService.starWorkflow(workflowId, CurrentUserHolder.get());
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{workflowId}/stars")
    public ApiResponse<Void> unstarWorkflow(@PathVariable Long workflowId) {
        workflowEngagementService.unstarWorkflow(workflowId, CurrentUserHolder.get());
        return ApiResponse.ok(null);
    }

    @PostMapping("/{workflowId}/favorites")
    public ApiResponse<Void> favoriteWorkflow(@PathVariable Long workflowId) {
        workflowEngagementService.favoriteWorkflow(workflowId, CurrentUserHolder.get());
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{workflowId}/favorites")
    public ApiResponse<Void> unfavoriteWorkflow(@PathVariable Long workflowId) {
        workflowEngagementService.unfavoriteWorkflow(workflowId, CurrentUserHolder.get());
        return ApiResponse.ok(null);
    }

    @GetMapping("/favorites")
    public ApiResponse<List<WorkflowListItemResponse>> listFavorites() {
        List<Long> workflowIds = workflowEngagementService.listFavoriteWorkflowIds(CurrentUserHolder.get().id());
        return ApiResponse.ok(workflowManagementService.listFavoriteWorkflows(workflowIds));
    }
}
