package com.toolshare.importexport.web;

import com.toolshare.importexport.model.ImportTaskRecord;

import java.time.OffsetDateTime;

public record ImportTaskResponse(
        Long id,
        String taskName,
        String fileName,
        String status,
        Integer successCount,
        Integer failureCount,
        String errorReportPath,
        String detail,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static ImportTaskResponse from(ImportTaskRecord record) {
        return new ImportTaskResponse(
                record.id(),
                record.taskName(),
                record.fileName(),
                record.status(),
                record.successCount(),
                record.failureCount(),
                record.errorReportPath(),
                record.detail(),
                record.createdAt(),
                record.updatedAt()
        );
    }
}
