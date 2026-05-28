package com.toolshare.importexport.model;

import java.time.OffsetDateTime;

public record ExportTaskRecord(
        Long id,
        String taskName,
        String filePath,
        String fileName,
        String status,
        Long requesterId,
        Integer successCount,
        Integer failureCount,
        String errorReportPath,
        String detail,
        Boolean deleted,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
