package com.toolshare.tool.web;

import com.toolshare.tag.model.TagRecord;
import com.toolshare.tool.model.ToolRecord;
import com.toolshare.tool.model.ToolSubmissionRecord;

import java.time.OffsetDateTime;
import java.util.List;

public record ToolDetailResponse(
        Long id,
        String name,
        String summary,
        String description,
        String url,
        String usageGuide,
        String status,
        Long recommenderId,
        String recommenderName,
        Integer starCount,
        Integer favoriteCount,
        Integer commentCount,
        Boolean starred,
        Boolean favorited,
        List<ToolListItemResponse.TagSnapshot> tags,
        SubmissionSnapshot submission,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static ToolDetailResponse from(ToolRecord tool,
                                          List<TagRecord> tags,
                                          ToolSubmissionRecord submission,
                                          boolean starred,
                                          boolean favorited) {
        return new ToolDetailResponse(
                tool.id(),
                tool.name(),
                tool.summary(),
                tool.description(),
                tool.url(),
                tool.usageGuide(),
                tool.status(),
                tool.recommenderId(),
                tool.recommenderName(),
                tool.starCount(),
                tool.favoriteCount(),
                tool.commentCount(),
                starred,
                favorited,
                tags.stream().map(ToolListItemResponse.TagSnapshot::from).toList(),
                submission == null ? null : SubmissionSnapshot.from(submission),
                tool.createdAt(),
                tool.updatedAt()
        );
    }

    public record SubmissionSnapshot(
            Long id,
            Long submitterId,
            String submitterName,
            String status,
            String remark,
            OffsetDateTime createdAt
    ) {

        public static SubmissionSnapshot from(ToolSubmissionRecord submission) {
            return new SubmissionSnapshot(
                    submission.id(),
                    submission.submitterId(),
                    submission.submitterName(),
                    submission.status(),
                    submission.remark(),
                    submission.createdAt()
            );
        }
    }
}
