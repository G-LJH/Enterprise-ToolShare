package com.toolshare.workflow.service;

import com.toolshare.exception.BadRequestException;
import com.toolshare.exception.NotFoundException;
import com.toolshare.model.AuditLogEntry;
import com.toolshare.security.CurrentUser;
import com.toolshare.user.repository.AuditLogRepository;
import com.toolshare.workflow.repository.WorkflowRepository;
import com.toolshare.workflow.repository.WorkflowSubmissionRepository;
import com.toolshare.workflow.repository.WorkflowSubmissionRepository.WorkflowSubmissionRecord;
import com.toolshare.workflow.web.WorkflowReviewResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkflowReviewService {

    private final WorkflowSubmissionRepository workflowSubmissionRepository;
    private final WorkflowRepository workflowRepository;
    private final WorkflowSubmissionStatusPolicy submissionStatusPolicy;
    private final AuditLogRepository auditLogRepository;

    public WorkflowReviewService(WorkflowSubmissionRepository workflowSubmissionRepository,
                                 WorkflowRepository workflowRepository,
                                 WorkflowSubmissionStatusPolicy submissionStatusPolicy,
                                 AuditLogRepository auditLogRepository) {
        this.workflowSubmissionRepository = workflowSubmissionRepository;
        this.workflowRepository = workflowRepository;
        this.submissionStatusPolicy = submissionStatusPolicy;
        this.auditLogRepository = auditLogRepository;
    }

    public java.util.List<WorkflowReviewResponse> listSubmissions(String status) {
        String normalizedStatus = submissionStatusPolicy.normalize(status);
        return workflowSubmissionRepository.searchReviewRecords(normalizedStatus).stream()
                .map(WorkflowReviewResponse::from)
                .toList();
    }

    @Transactional
    public WorkflowReviewResponse approveSubmission(Long submissionId, com.toolshare.review.web.ReviewDecisionRequest request, CurrentUser operator) {
        WorkflowSubmissionRecord submission = getSubmissionOrThrow(submissionId);
        submissionStatusPolicy.assertPending(submission.status());
        String remark = normalizeOptionalRemark(request == null ? null : request.remark(), "审核通过");
        workflowSubmissionRepository.updateStatusAndRemark(submissionId, WorkflowSubmissionStatusPolicy.APPROVED, remark, operator.id());
        workflowRepository.updateStatus(submission.workflowId(), WorkflowStatusPolicy.APPROVED, operator.id());
        auditLogRepository.insert(new AuditLogEntry(
                "WORKFLOW_REVIEW_APPROVED",
                "WORKFLOW_SUBMISSION",
                String.valueOf(submissionId),
                operator.id(),
                operator.username(),
                "approved workflow " + submission.workflowName()
        ));
        return listSingleSubmission(submissionId);
    }

    @Transactional
    public WorkflowReviewResponse rejectSubmission(Long submissionId, com.toolshare.review.web.ReviewDecisionRequest request, CurrentUser operator) {
        WorkflowSubmissionRecord submission = getSubmissionOrThrow(submissionId);
        submissionStatusPolicy.assertPending(submission.status());
        String remark = normalizeRequiredRemark(request == null ? null : request.remark());
        workflowSubmissionRepository.updateStatusAndRemark(submissionId, WorkflowSubmissionStatusPolicy.REJECTED, remark, operator.id());
        workflowRepository.updateStatus(submission.workflowId(), WorkflowStatusPolicy.REJECTED, operator.id());
        auditLogRepository.insert(new AuditLogEntry(
                "WORKFLOW_REVIEW_REJECTED",
                "WORKFLOW_SUBMISSION",
                String.valueOf(submissionId),
                operator.id(),
                operator.username(),
                "rejected workflow " + submission.workflowName()
        ));
        return listSingleSubmission(submissionId);
    }

    private WorkflowReviewResponse listSingleSubmission(Long submissionId) {
        WorkflowSubmissionRecord record = workflowSubmissionRepository.findById(submissionId)
                .orElseThrow(() -> new NotFoundException("提交单不存在"));
        return WorkflowReviewResponse.from(record);
    }

    private WorkflowSubmissionRecord getSubmissionOrThrow(Long submissionId) {
        return workflowSubmissionRepository.findById(submissionId)
                .orElseThrow(() -> new NotFoundException("提交单不存在"));
    }

    private String normalizeOptionalRemark(String remark, String fallback) {
        if (remark == null || remark.isBlank()) {
            return fallback;
        }
        String normalized = remark.trim();
        if (normalized.length() > 1024) {
            throw new BadRequestException("审核备注长度超出限制");
        }
        return normalized;
    }

    private String normalizeRequiredRemark(String remark) {
        if (remark == null || remark.isBlank()) {
            throw new BadRequestException("驳回原因不能为空");
        }
        return normalizeOptionalRemark(remark, "");
    }
}
