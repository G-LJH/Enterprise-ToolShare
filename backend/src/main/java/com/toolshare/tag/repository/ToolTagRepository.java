package com.toolshare.tag.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ToolTagRepository {

    private final JdbcTemplate jdbcTemplate;

    public ToolTagRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void replaceToolTags(Long toolId, List<Long> tagIds) {
        jdbcTemplate.update("DELETE FROM tool_tags WHERE tool_id = ?", toolId);
        tagIds.forEach(tagId -> jdbcTemplate.update("""
                        INSERT INTO tool_tags (tool_id, tag_id)
                        VALUES (?, ?)
                        """,
                toolId,
                tagId
        ));
    }

    public void deleteByTagId(Long tagId) {
        jdbcTemplate.update("DELETE FROM tool_tags WHERE tag_id = ?", tagId);
    }
}
