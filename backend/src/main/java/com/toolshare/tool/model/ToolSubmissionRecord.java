package com.toolshare.tool.model;

import java.time.OffsetDateTime;

public record ToolSubmissionRecord(
        Long id,
        Long toolId,
        Long submitterId,
        String submitterName,
        String status,
        String remark,
        Boolean deleted,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
