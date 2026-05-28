package com.toolshare.log.web;

import com.toolshare.log.model.OperationLogRecord;

import java.time.OffsetDateTime;

public record OperationLogItemResponse(
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
    public static OperationLogItemResponse from(OperationLogRecord record) {
        return new OperationLogItemResponse(
                record.id(),
                record.operation(),
                record.module(),
                record.userId(),
                record.username(),
                record.realName(),
                record.success(),
                record.message(),
                record.createdAt()
        );
    }
}
