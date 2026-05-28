package com.toolshare.tool.web;

import com.toolshare.tag.model.TagRecord;

import com.toolshare.tool.model.ToolRecord;

import java.time.OffsetDateTime;
import java.util.List;

public record ToolListItemResponse(
        Long id,
        String name,
        String summary,
        String description,
        String url,
        String status,
        Long recommenderId,
        String recommenderName,
        Integer starCount,
        Integer favoriteCount,
        Integer commentCount,
        List<TagSnapshot> tags,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static ToolListItemResponse from(ToolRecord record, List<TagRecord> tags) {
        return new ToolListItemResponse(
                record.id(),
                record.name(),
                record.summary(),
                record.description(),
                record.url(),
                record.status(),
                record.recommenderId(),
                record.recommenderName(),
                record.starCount(),
                record.favoriteCount(),
                record.commentCount(),
                tags.stream().map(TagSnapshot::from).toList(),
                record.createdAt(),
                record.updatedAt()
        );
    }

    public record TagSnapshot(
            Long id,
            String name
    ) {

        public static TagSnapshot from(TagRecord tag) {
            return new TagSnapshot(tag.id(), tag.name());
        }
    }
}
