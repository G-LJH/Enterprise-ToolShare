package com.toolshare.importexport.web;

import com.toolshare.importexport.model.ExportTaskRecord;

import java.time.OffsetDateTime;

public record ExportTaskResponse(
        Long id,
        String taskName,
        String fileName,
        String status,
        Integer successCount,
        Integer failureCount,
        String errorReportPath,
        String detail,
        String content,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static ExportTaskResponse from(ExportTaskRecord record, String content) {
        return new ExportTaskResponse(
                record.id(),
                record.taskName(),
                record.fileName(),
                record.status(),
                record.successCount(),
                record.failureCount(),
                record.errorReportPath(),
                record.detail(),
                content,
                record.createdAt(),
                record.updatedAt()
        );
    }

    public static ExportTaskResponse from(ExportTaskRecord record) {
        return from(record, null);
    }
}
