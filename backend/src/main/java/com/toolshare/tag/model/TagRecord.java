package com.toolshare.tag.model;

import java.time.OffsetDateTime;

public record TagRecord(
        Long id,
        String name,
        String description,
        Boolean deleted,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
