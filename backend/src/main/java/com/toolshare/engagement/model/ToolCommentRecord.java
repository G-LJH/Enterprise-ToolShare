package com.toolshare.engagement.model;

import java.time.OffsetDateTime;

public record ToolCommentRecord(
        Long id,
        Long toolId,
        Long userId,
        String authorName,
        String content,
        String status,
        Boolean deleted,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
