package com.toolshare.log.model;

import java.time.OffsetDateTime;

public record AuditLogRecord(
        Long id,
        String action,
        String objectType,
        String objectId,
        Long operatorId,
        String operatorName,
        String detail,
        OffsetDateTime createdAt
) {
}
