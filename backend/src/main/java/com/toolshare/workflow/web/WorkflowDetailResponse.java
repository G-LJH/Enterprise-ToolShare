package com.toolshare.workflow.web;

import com.toolshare.tag.model.TagRecord;
import com.toolshare.tool.model.ToolRecord;
import com.toolshare.workflow.model.WorkflowRecord;

import java.time.OffsetDateTime;
import java.util.List;

public record WorkflowDetailResponse(
        Long id,
        String name,
        String scenario,
        String description,
        String steps,
        boolean featured,
        String status,
        Long creatorId,
        String creatorName,
        Integer starCount,
        Integer favoriteCount,
        boolean starred,
        boolean favorited,
        List<WorkflowToolDetail> tools,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static WorkflowDetailResponse from(WorkflowRecord workflow,
                                              List<WorkflowToolDetail> tools,
                                              boolean starred,
                                              boolean favorited) {
        return new WorkflowDetailResponse(
                workflow.id(),
                workflow.name(),
                workflow.scenario(),
                workflow.description(),
                workflow.steps(),
                workflow.featured(),
                workflow.status(),
                workflow.creatorId(),
                workflow.creatorName(),
                workflow.starCount(),
                workflow.favoriteCount(),
                starred,
                favorited,
                tools,
                workflow.createdAt(),
                workflow.updatedAt()
        );
    }

    public record WorkflowToolDetail(
            Long id,
            String name,
            String summary,
            String description,
            String url,
            List<WorkflowTagSnapshot> tags
    ) {

        public static WorkflowToolDetail from(ToolRecord tool, List<TagRecord> tags) {
            return new WorkflowToolDetail(
                    tool.id(),
                    tool.name(),
                    tool.summary(),
                    tool.description(),
                    tool.url(),
                    tags.stream().map(WorkflowTagSnapshot::from).toList()
            );
        }
    }

    public record WorkflowTagSnapshot(
            Long id,
            String name
    ) {

        public static WorkflowTagSnapshot from(TagRecord tag) {
            return new WorkflowTagSnapshot(tag.id(), tag.name());
        }
    }
}
