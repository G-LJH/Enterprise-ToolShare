package com.toolshare.workflow.web;

import com.toolshare.workflow.repository.WorkflowSubmissionRepository.WorkflowSubmissionRecord;

import java.time.OffsetDateTime;

public record WorkflowReviewResponse(
        Long submissionId,
        Long workflowId,
        String workflowName,
        String workflowScenario,
        String workflowDescription,
        String workflowSteps,
        boolean workflowFeatured,
        Integer workflowStarCount,
        Integer workflowFavoriteCount,
        Long submitterId,
        String submissionStatus,
        String remark,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static WorkflowReviewResponse from(WorkflowSubmissionRecord record) {
        return new WorkflowReviewResponse(
                record.id(),
                record.workflowId(),
                record.workflowName(),
                record.workflowScenario(),
                record.workflowDescription(),
                record.workflowSteps(),
                record.workflowFeatured(),
                record.workflowStarCount(),
                record.workflowFavoriteCount(),
                record.submitterId(),
                record.status(),
                record.remark(),
                null,
                null
        );
    }
}
