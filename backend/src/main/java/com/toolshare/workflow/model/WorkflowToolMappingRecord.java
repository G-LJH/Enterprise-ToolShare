package com.toolshare.workflow.model;

public record WorkflowToolMappingRecord(
        Long workflowId,
        Long toolId,
        Integer sortOrder
) {
}
