package com.toolshare.workflow.service;

import com.toolshare.exception.BadRequestException;
import com.toolshare.exception.NotFoundException;
import com.toolshare.model.AuditLogEntry;
import com.toolshare.security.CurrentUser;
import com.toolshare.systemconfig.service.SystemConfigService;
import com.toolshare.tag.model.TagRecord;
import com.toolshare.tag.repository.TagRepository;
import com.toolshare.tool.model.ToolRecord;
import com.toolshare.tool.repository.ToolRepository;
import com.toolshare.user.repository.AuditLogRepository;
import com.toolshare.workflow.model.WorkflowRecord;
import com.toolshare.workflow.repository.WorkflowRepository;
import com.toolshare.workflow.repository.WorkflowSubmissionRepository;
import com.toolshare.workflow.repository.WorkflowToolRepository;
import com.toolshare.workflow.web.WorkflowDetailResponse;
import com.toolshare.workflow.web.WorkflowListItemResponse;
import com.toolshare.workflow.web.WorkflowSubmissionRequest;
import com.toolshare.workflow.web.WorkflowUpsertRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class WorkflowManagementService {

    private final WorkflowRepository workflowRepository;
    private final WorkflowToolRepository workflowToolRepository;
    private final WorkflowSubmissionRepository workflowSubmissionRepository;
    private final ToolRepository toolRepository;
    private final TagRepository tagRepository;
    private final AuditLogRepository auditLogRepository;
    private final WorkflowEngagementService workflowEngagementService;
    private final WorkflowStatusPolicy workflowStatusPolicy;
    private final WorkflowSubmissionStatusPolicy submissionStatusPolicy;
    private final SystemConfigService systemConfigService;

    public WorkflowManagementService(WorkflowRepository workflowRepository,
                                     WorkflowToolRepository workflowToolRepository,
                                     WorkflowSubmissionRepository workflowSubmissionRepository,
                                     ToolRepository toolRepository,
                                     TagRepository tagRepository,
                                     AuditLogRepository auditLogRepository,
                                     WorkflowEngagementService workflowEngagementService,
                                     WorkflowStatusPolicy workflowStatusPolicy,
                                     WorkflowSubmissionStatusPolicy submissionStatusPolicy,
                                     SystemConfigService systemConfigService) {
        this.workflowRepository = workflowRepository;
        this.workflowToolRepository = workflowToolRepository;
        this.workflowSubmissionRepository = workflowSubmissionRepository;
        this.toolRepository = toolRepository;
        this.tagRepository = tagRepository;
        this.auditLogRepository = auditLogRepository;
        this.workflowEngagementService = workflowEngagementService;
        this.workflowStatusPolicy = workflowStatusPolicy;
        this.submissionStatusPolicy = submissionStatusPolicy;
        this.systemConfigService = systemConfigService;
    }

    public List<WorkflowListItemResponse> listFavoriteWorkflows(List<Long> workflowIds) {
        if (workflowIds == null || workflowIds.isEmpty()) {
            return List.of();
        }
        List<WorkflowRecord> workflows = workflowRepository.findByIds(workflowIds);
        return toListResponses(workflows);
    }

    public List<WorkflowListItemResponse> listVisibleWorkflows(String scenario, Boolean featuredOnly) {
        List<WorkflowRecord> workflows = workflowRepository.searchVisible(scenario, featuredOnly);
        return toListResponses(workflows);
    }

    public List<WorkflowListItemResponse> listAdminWorkflows(String scenario) {
        return toListResponses(workflowRepository.searchForAdmin(scenario));
    }

    public WorkflowDetailResponse getVisibleWorkflowDetail(Long workflowId) {
        return buildDetail(getWorkflowOrThrow(workflowId), null);
    }

    public WorkflowDetailResponse getAdminWorkflowDetail(Long workflowId) {
        return buildDetail(getWorkflowOrThrow(workflowId), null);
    }

    public WorkflowDetailResponse getWorkflowDetailForUser(Long workflowId, CurrentUser currentUser) {
        return buildDetail(getWorkflowOrThrow(workflowId), currentUser);
    }

    @Transactional
    public WorkflowDetailResponse createWorkflow(WorkflowUpsertRequest request, CurrentUser operator) {
        ValidatedWorkflowInput input = validateInput(request);
        List<ToolRecord> tools = validateToolIds(input.toolIds());
        long workflowId = workflowRepository.insert(
                input.name(),
                input.scenario(),
                input.description(),
                input.steps(),
                input.featured(),
                operator.id(),
                operator.id()
        );
        workflowToolRepository.replaceWorkflowTools(workflowId, tools.stream().map(ToolRecord::id).toList());
        auditLogRepository.insert(new AuditLogEntry(
                "WORKFLOW_CREATED",
                "WORKFLOW",
                String.valueOf(workflowId),
                operator.id(),
                operator.username(),
                "created workflow " + input.name()
        ));
        return getAdminWorkflowDetail(workflowId);
    }

    @Transactional
    public WorkflowDetailResponse submitWorkflow(WorkflowSubmissionRequest request, CurrentUser operator) {
        boolean reviewEnabled = systemConfigService.isWorkflowReviewEnabled();
        String workflowStatus = reviewEnabled ? workflowStatusPolicy.initialSubmissionStatus() : WorkflowStatusPolicy.APPROVED;
        String submissionStatus = reviewEnabled ? submissionStatusPolicy.initialSubmissionStatus() : WorkflowSubmissionStatusPolicy.APPROVED;
        String submissionRemark = reviewEnabled ? "等待管理员审核" : "审核开关关闭，系统自动通过";
        
        ValidatedWorkflowInput input = validateInput(new WorkflowUpsertRequest(
                request.name(),
                request.scenario(),
                request.description(),
                request.steps(),
                false,
                request.toolIds()
        ));
        List<ToolRecord> tools = validateToolIds(input.toolIds());
        
        long workflowId = workflowRepository.insert(
                input.name(),
                input.scenario(),
                input.description(),
                input.steps(),
                input.featured(),
                workflowStatus,
                operator.id(),
                operator.id()
        );
        workflowToolRepository.replaceWorkflowTools(workflowId, tools.stream().map(ToolRecord::id).toList());
        long submissionId = workflowSubmissionRepository.insert(workflowId, operator.id(), submissionStatus, submissionRemark, operator.id());
        
        auditLogRepository.insert(new AuditLogEntry(
                "WORKFLOW_SUBMITTED",
                "WORKFLOW",
                String.valueOf(workflowId),
                operator.id(),
                operator.username(),
                "submitted workflow " + input.name() + ", reviewEnabled=" + reviewEnabled
        ));
        return getWorkflowDetailForUser(workflowId, operator);
    }

    @Transactional
    public WorkflowDetailResponse updateWorkflow(Long workflowId, WorkflowUpsertRequest request, CurrentUser operator) {
        WorkflowRecord existing = getWorkflowOrThrow(workflowId);
        ValidatedWorkflowInput input = validateInput(request);
        List<ToolRecord> tools = validateToolIds(input.toolIds());
        workflowRepository.updateWorkflow(
                workflowId,
                input.name(),
                input.scenario(),
                input.description(),
                input.steps(),
                input.featured(),
                operator.id()
        );
        workflowToolRepository.replaceWorkflowTools(workflowId, tools.stream().map(ToolRecord::id).toList());
        auditLogRepository.insert(new AuditLogEntry(
                "WORKFLOW_UPDATED",
                "WORKFLOW",
                String.valueOf(workflowId),
                operator.id(),
                operator.username(),
                "updated workflow " + existing.name()
        ));
        return getAdminWorkflowDetail(workflowId);
    }

    @Transactional
    public void deleteWorkflow(Long workflowId, CurrentUser operator) {
        WorkflowRecord existing = getWorkflowOrThrow(workflowId);
        workflowRepository.softDelete(workflowId, operator.id());
        auditLogRepository.insert(new AuditLogEntry(
                "WORKFLOW_DELETED",
                "WORKFLOW",
                String.valueOf(workflowId),
                operator.id(),
                operator.username(),
                "deleted workflow " + existing.name()
        ));
    }

    private List<WorkflowListItemResponse> toListResponses(List<WorkflowRecord> workflows) {
        Map<Long, List<ToolRecord>> toolsByWorkflowId = getOrderedToolsByWorkflowIds(workflows.stream().map(WorkflowRecord::id).toList());
        return workflows.stream()
                .map(workflow -> WorkflowListItemResponse.from(workflow, toolsByWorkflowId.getOrDefault(workflow.id(), List.of())))
                .toList();
    }

    private WorkflowDetailResponse buildDetail(WorkflowRecord workflow, CurrentUser currentUser) {
        List<ToolRecord> orderedTools = getOrderedToolsByWorkflowIds(List.of(workflow.id())).getOrDefault(workflow.id(), List.of());
        Map<Long, List<TagRecord>> tagsByToolId = tagRepository.findByToolIds(orderedTools.stream().map(ToolRecord::id).toList());
        List<WorkflowDetailResponse.WorkflowToolDetail> toolDetails = orderedTools.stream()
                .map(tool -> WorkflowDetailResponse.WorkflowToolDetail.from(tool, tagsByToolId.getOrDefault(tool.id(), List.of())))
                .toList();
        boolean starred = currentUser != null && workflowEngagementService.hasStarred(workflow.id(), currentUser.id());
        boolean favorited = currentUser != null && workflowEngagementService.hasFavorited(workflow.id(), currentUser.id());
        return WorkflowDetailResponse.from(workflow, toolDetails, starred, favorited);
    }

    private Map<Long, List<ToolRecord>> getOrderedToolsByWorkflowIds(List<Long> workflowIds) {
        Map<Long, List<Long>> toolIdsByWorkflowId = workflowToolRepository.findToolIdsByWorkflowIds(workflowIds);
        List<Long> allToolIds = toolIdsByWorkflowId.values().stream().flatMap(List::stream).distinct().toList();
        Map<Long, ToolRecord> toolsById = new LinkedHashMap<>();
        toolRepository.findByIds(allToolIds).forEach(tool -> toolsById.put(tool.id(), tool));

        Map<Long, List<ToolRecord>> result = new LinkedHashMap<>();
        toolIdsByWorkflowId.forEach((workflowId, toolIds) -> {
            List<ToolRecord> orderedTools = new ArrayList<>();
            for (Long toolId : toolIds) {
                ToolRecord tool = toolsById.get(toolId);
                if (tool != null) {
                    orderedTools.add(tool);
                }
            }
            result.put(workflowId, orderedTools);
        });
        return result;
    }

    private List<ToolRecord> validateToolIds(List<Long> toolIds) {
        if (toolIds == null || toolIds.isEmpty()) {
            throw new BadRequestException("至少关联一个工具");
        }
        List<Long> distinctIds = toolIds.stream().distinct().toList();
        Map<Long, ToolRecord> toolsById = new LinkedHashMap<>();
        toolRepository.findByIds(distinctIds).forEach(tool -> toolsById.put(tool.id(), tool));
        if (toolsById.size() != distinctIds.size()) {
            throw new BadRequestException("存在无效的工具关联");
        }
        List<ToolRecord> orderedTools = new ArrayList<>();
        for (Long toolId : distinctIds) {
            ToolRecord tool = toolsById.get(toolId);
            if (tool != null) {
                orderedTools.add(tool);
            }
        }
        return orderedTools;
    }

    private WorkflowRecord getWorkflowOrThrow(Long workflowId) {
        return workflowRepository.findActiveById(workflowId)
                .orElseThrow(() -> new NotFoundException("工作流不存在"));
    }

    private ValidatedWorkflowInput validateInput(WorkflowUpsertRequest request) {
        String name = normalizeRequired(request.name(), "工作流名称不能为空", 256);
        String scenario = normalizeRequired(request.scenario(), "适用场景不能为空", 256);
        String description = normalizeRequired(request.description(), "工作流描述不能为空", 4000);
        String steps = normalizeRequired(request.steps(), "步骤说明不能为空", 8000);
        boolean featured = Boolean.TRUE.equals(request.featured());
        return new ValidatedWorkflowInput(name, scenario, description, steps, featured, request.toolIds() == null ? List.of() : request.toolIds());
    }

    private String normalizeRequired(String value, String message, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(message);
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new BadRequestException("输入内容长度超出限制");
        }
        return normalized;
    }

    private record ValidatedWorkflowInput(
            String name,
            String scenario,
            String description,
            String steps,
            boolean featured,
            List<Long> toolIds
    ) {
    }
}
