package com.toolshare.systemconfig.model;

import java.time.OffsetDateTime;

public record SystemConfigRecord(
        Long id,
        String configKey,
        String configValue,
        String description,
        Boolean deleted,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
