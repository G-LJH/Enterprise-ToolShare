package com.toolshare.tool.model;

import java.time.OffsetDateTime;

public record ToolRecord(
        Long id,
        String name,
        String summary,
        String description,
        String url,
        String usageGuide,
        Long recommenderId,
        String recommenderName,
        String status,
        Integer starCount,
        Integer favoriteCount,
        Integer commentCount,
        Boolean deleted,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
