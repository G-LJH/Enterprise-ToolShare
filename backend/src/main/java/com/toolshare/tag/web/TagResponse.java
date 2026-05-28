package com.toolshare.tag.web;

import com.toolshare.tag.model.TagRecord;

import java.time.OffsetDateTime;

public record TagResponse(
        Long id,
        String name,
        String description,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static TagResponse from(TagRecord record) {
        return new TagResponse(
                record.id(),
                record.name(),
                record.description(),
                record.createdAt(),
                record.updatedAt()
        );
    }
}
