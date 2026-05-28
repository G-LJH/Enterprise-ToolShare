package com.toolshare.model;

public record AuditLogEntry(
        String action,
        String objectType,
        String objectId,
        Long operatorId,
        String operatorName,
        String detail
) {
}
