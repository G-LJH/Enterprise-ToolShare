package com.toolshare.review.model;

import java.time.OffsetDateTime;

public record ToolReviewRecord(
        Long submissionId,
        Long toolId,
        Long submitterId,
        String submitterName,
        String toolName,
        String toolSummary,
        String toolStatus,
        String submissionStatus,
        String remark,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
