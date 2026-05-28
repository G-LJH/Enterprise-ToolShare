package com.toolshare.review.web;

import com.toolshare.review.model.ToolReviewRecord;
import com.toolshare.tag.model.TagRecord;
import com.toolshare.tool.web.ToolListItemResponse;

import java.time.OffsetDateTime;
import java.util.List;

public record ToolReviewResponse(
        Long submissionId,
        Long toolId,
        Long submitterId,
        String submitterName,
        String toolName,
        String toolSummary,
        String toolStatus,
        String submissionStatus,
        String remark,
        List<ToolListItemResponse.TagSnapshot> tags,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static ToolReviewResponse from(ToolReviewRecord record, List<TagRecord> tags) {
        return new ToolReviewResponse(
                record.submissionId(),
                record.toolId(),
                record.submitterId(),
                record.submitterName(),
                record.toolName(),
                record.toolSummary(),
                record.toolStatus(),
                record.submissionStatus(),
                record.remark(),
                tags.stream().map(ToolListItemResponse.TagSnapshot::from).toList(),
                record.createdAt(),
                record.updatedAt()
        );
    }
}
