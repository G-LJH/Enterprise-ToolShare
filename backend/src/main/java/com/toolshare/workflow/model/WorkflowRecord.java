package com.toolshare.workflow.model;

import java.time.OffsetDateTime;

public record WorkflowRecord(
        Long id,
        String name,
        String scenario,
        String description,
        String steps,
        Boolean featured,
        String status,
        Long creatorId,
        String creatorName,
        Integer starCount,
        Integer favoriteCount,
        Boolean deleted,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
