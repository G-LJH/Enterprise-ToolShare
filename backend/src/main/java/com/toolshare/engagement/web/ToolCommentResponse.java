package com.toolshare.engagement.web;

import com.toolshare.engagement.model.ToolCommentRecord;

import java.time.OffsetDateTime;

public record ToolCommentResponse(
        Long id,
        Long toolId,
        Long userId,
        String authorName,
        String content,
        OffsetDateTime createdAt,
        boolean deletable
) {

    public static ToolCommentResponse from(ToolCommentRecord record, boolean deletable) {
        return new ToolCommentResponse(
                record.id(),
                record.toolId(),
                record.userId(),
                record.authorName(),
                record.content(),
                record.createdAt(),
                deletable
        );
    }
}
