package com.toolshare.workflow.web;

import com.toolshare.model.ApiResponse;
import com.toolshare.security.CurrentUserHolder;
import com.toolshare.security.RequireAuthenticated;
import com.toolshare.workflow.service.WorkflowManagementService;
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
@RequestMapping("/api/workflows")
public class WorkflowController {

    private final WorkflowManagementService workflowManagementService;

    public WorkflowController(WorkflowManagementService workflowManagementService) {
        this.workflowManagementService = workflowManagementService;
    }

    @GetMapping
    public ApiResponse<List<WorkflowListItemResponse>> listWorkflows(@RequestParam(required = false) String scenario,
                                                                     @RequestParam(required = false) Boolean featuredOnly) {
        return ApiResponse.ok(workflowManagementService.listVisibleWorkflows(scenario, featuredOnly));
    }

    @GetMapping("/{workflowId}")
    public ApiResponse<WorkflowDetailResponse> getWorkflow(@PathVariable Long workflowId) {
        return ApiResponse.ok(workflowManagementService.getVisibleWorkflowDetail(workflowId));
    }

    @PostMapping("/submissions")
    @RequireAuthenticated
    public ApiResponse<WorkflowDetailResponse> submitWorkflow(@Valid @RequestBody WorkflowSubmissionRequest request) {
        return ApiResponse.ok(workflowManagementService.submitWorkflow(request, CurrentUserHolder.get()));
    }
}
