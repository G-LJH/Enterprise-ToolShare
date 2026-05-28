package com.toolshare.model;

public record OperationLogEntry(
        String operation,
        String module,
        Long userId,
        boolean success,
        String message
) {
}
