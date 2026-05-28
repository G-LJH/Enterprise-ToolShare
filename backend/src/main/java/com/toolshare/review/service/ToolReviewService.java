package com.toolshare.review.service;

import com.toolshare.exception.BadRequestException;
import com.toolshare.exception.NotFoundException;
import com.toolshare.model.AuditLogEntry;
import com.toolshare.review.model.ToolReviewRecord;
import com.toolshare.review.web.ReviewDecisionRequest;
import com.toolshare.review.web.ToolReviewResponse;
import com.toolshare.security.CurrentUser;
import com.toolshare.tag.model.TagRecord;
import com.toolshare.tag.repository.TagRepository;
import com.toolshare.tool.model.ToolRecord;
import com.toolshare.tool.model.ToolSubmissionRecord;
import com.toolshare.tool.repository.ToolRepository;
import com.toolshare.tool.repository.ToolSubmissionRepository;
import com.toolshare.tool.service.ToolStatusPolicy;
import com.toolshare.user.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class ToolReviewService {

    private final ToolSubmissionRepository toolSubmissionRepository;
    private final ToolRepository toolRepository;
    private final TagRepository tagRepository;
    private final SubmissionStatusPolicy submissionStatusPolicy;
    private final AuditLogRepository auditLogRepository;

    public ToolReviewService(ToolSubmissionRepository toolSubmissionRepository,
                             ToolRepository toolRepository,
                             TagRepository tagRepository,
                             SubmissionStatusPolicy submissionStatusPolicy,
                             AuditLogRepository auditLogRepository) {
        this.toolSubmissionRepository = toolSubmissionRepository;
        this.toolRepository = toolRepository;
        this.tagRepository = tagRepository;
        this.submissionStatusPolicy = submissionStatusPolicy;
        this.auditLogRepository = auditLogRepository;
    }

    public List<ToolReviewResponse> listSubmissions(String status) {
        String normalizedStatus = submissionStatusPolicy.normalize(status);
        List<ToolReviewRecord> records = toolSubmissionRepository.searchReviewRecords(normalizedStatus);
        Map<Long, List<TagRecord>> tagsByToolId = tagRepository.findByToolIds(records.stream().map(ToolReviewRecord::toolId).distinct().toList());
        return records.stream()
                .map(record -> ToolReviewResponse.from(record, tagsByToolId.getOrDefault(record.toolId(), List.of())))
                .toList();
    }

    @Transactional
    public ToolReviewResponse approveSubmission(Long submissionId, ReviewDecisionRequest request, CurrentUser operator) {
        ToolSubmissionRecord submission = getSubmissionOrThrow(submissionId);
        submissionStatusPolicy.assertPending(submission.status());
        ToolRecord tool = getToolOrThrow(submission.toolId());
        String remark = normalizeOptionalRemark(request == null ? null : request.remark(), "审核通过");
        toolSubmissionRepository.updateStatusAndRemark(submissionId, SubmissionStatusPolicy.APPROVED, remark, operator.id());
        toolRepository.updateStatus(tool.id(), ToolStatusPolicy.APPROVED, operator.id());
        auditLogRepository.insert(new AuditLogEntry(
                "TOOL_REVIEW_APPROVED",
                "TOOL_SUBMISSION",
                String.valueOf(submissionId),
                operator.id(),
                operator.username(),
                "approved tool " + tool.name()
        ));
        return listSingleSubmission(submissionId);
    }

    @Transactional
    public ToolReviewResponse rejectSubmission(Long submissionId, ReviewDecisionRequest request, CurrentUser operator) {
        ToolSubmissionRecord submission = getSubmissionOrThrow(submissionId);
        submissionStatusPolicy.assertPending(submission.status());
        ToolRecord tool = getToolOrThrow(submission.toolId());
        String remark = normalizeRequiredRemark(request == null ? null : request.remark());
        toolSubmissionRepository.updateStatusAndRemark(submissionId, SubmissionStatusPolicy.REJECTED, remark, operator.id());
        toolRepository.updateStatus(tool.id(), ToolStatusPolicy.REJECTED, operator.id());
        auditLogRepository.insert(new AuditLogEntry(
                "TOOL_REVIEW_REJECTED",
                "TOOL_SUBMISSION",
                String.valueOf(submissionId),
                operator.id(),
                operator.username(),
                "rejected tool " + tool.name()
        ));
        return listSingleSubmission(submissionId);
    }

    private ToolReviewResponse listSingleSubmission(Long submissionId) {
        ToolReviewRecord record = toolSubmissionRepository.findReviewRecordById(submissionId)
                .orElseThrow(() -> new NotFoundException("提交单不存在"));
        List<TagRecord> tags = tagRepository.findByToolIds(List.of(record.toolId())).getOrDefault(record.toolId(), List.of());
        return ToolReviewResponse.from(record, tags);
    }

    private ToolSubmissionRecord getSubmissionOrThrow(Long submissionId) {
        return toolSubmissionRepository.findById(submissionId)
                .orElseThrow(() -> new NotFoundException("提交单不存在"));
    }

    private ToolRecord getToolOrThrow(Long toolId) {
        return toolRepository.findActiveById(toolId)
                .orElseThrow(() -> new NotFoundException("工具不存在"));
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
