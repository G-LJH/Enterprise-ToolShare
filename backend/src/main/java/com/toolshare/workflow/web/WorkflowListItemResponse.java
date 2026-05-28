package com.toolshare.workflow.web;

import com.toolshare.tool.model.ToolRecord;
import com.toolshare.workflow.model.WorkflowRecord;

import java.time.OffsetDateTime;
import java.util.List;

public record WorkflowListItemResponse(
        Long id,
        String name,
        String scenario,
        String description,
        boolean featured,
        Long creatorId,
        String creatorName,
        int toolCount,
        List<ToolSnapshot> tools,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static WorkflowListItemResponse from(WorkflowRecord workflow, List<ToolRecord> tools) {
        return new WorkflowListItemResponse(
                workflow.id(),
                workflow.name(),
                workflow.scenario(),
                workflow.description(),
                workflow.featured(),
                workflow.creatorId(),
                workflow.creatorName(),
                tools.size(),
                tools.stream().map(ToolSnapshot::from).toList(),
                workflow.createdAt(),
                workflow.updatedAt()
        );
    }

    public record ToolSnapshot(
            Long id,
            String name,
            String summary,
            String url
    ) {

        public static ToolSnapshot from(ToolRecord tool) {
            return new ToolSnapshot(tool.id(), tool.name(), tool.summary(), tool.url());
        }
    }
}
