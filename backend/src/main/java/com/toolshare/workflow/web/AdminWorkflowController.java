package com.toolshare.workflow.web;

import com.toolshare.model.ApiResponse;
import com.toolshare.security.CurrentUserHolder;
import com.toolshare.security.RequireRole;
import com.toolshare.user.repository.RoleRepository;
import com.toolshare.workflow.service.WorkflowManagementService;
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
@RequestMapping("/api/admin/workflows")
@RequireRole(RoleRepository.ADMIN)
public class AdminWorkflowController {

    private final WorkflowManagementService workflowManagementService;

    public AdminWorkflowController(WorkflowManagementService workflowManagementService) {
        this.workflowManagementService = workflowManagementService;
    }

    @GetMapping
    public ApiResponse<List<WorkflowListItemResponse>> listWorkflows(@RequestParam(required = false) String scenario) {
        return ApiResponse.ok(workflowManagementService.listAdminWorkflows(scenario));
    }

    @GetMapping("/{workflowId}")
    public ApiResponse<WorkflowDetailResponse> getWorkflow(@PathVariable Long workflowId) {
        return ApiResponse.ok(workflowManagementService.getAdminWorkflowDetail(workflowId));
    }

    @PostMapping
    public ApiResponse<WorkflowDetailResponse> createWorkflow(@Valid @RequestBody WorkflowUpsertRequest request) {
        return ApiResponse.ok(workflowManagementService.createWorkflow(request, CurrentUserHolder.get()));
    }

    @PutMapping("/{workflowId}")
    public ApiResponse<WorkflowDetailResponse> updateWorkflow(@PathVariable Long workflowId,
                                                              @Valid @RequestBody WorkflowUpsertRequest request) {
        return ApiResponse.ok(workflowManagementService.updateWorkflow(workflowId, request, CurrentUserHolder.get()));
    }

    @DeleteMapping("/{workflowId}")
    public ApiResponse<Void> deleteWorkflow(@PathVariable Long workflowId) {
        workflowManagementService.deleteWorkflow(workflowId, CurrentUserHolder.get());
        return ApiResponse.ok(null);
    }
}
