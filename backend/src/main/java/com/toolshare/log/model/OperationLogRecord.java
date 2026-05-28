package com.toolshare.log.model;

import java.time.OffsetDateTime;

public record OperationLogRecord(
        Long id,
        String operation,
        String module,
        Long userId,
        String username,
        String realName,
        boolean success,
        String message,
        OffsetDateTime createdAt
) {
}
