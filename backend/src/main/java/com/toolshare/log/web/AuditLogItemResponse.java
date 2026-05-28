package com.toolshare.log.web;

import com.toolshare.log.model.AuditLogRecord;

import java.time.OffsetDateTime;

public record AuditLogItemResponse(
        Long id,
        String action,
        String objectType,
        String objectId,
        Long operatorId,
        String operatorName,
        String detail,
        OffsetDateTime createdAt
) {
    public static AuditLogItemResponse from(AuditLogRecord record) {
        return new AuditLogItemResponse(
                record.id(),
                record.action(),
                record.objectType(),
                record.objectId(),
                record.operatorId(),
                record.operatorName(),
                record.detail(),
                record.createdAt()
        );
    }
}
