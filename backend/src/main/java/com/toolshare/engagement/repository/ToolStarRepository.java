package com.toolshare.engagement.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ToolStarRepository {

    private final JdbcTemplate jdbcTemplate;

    public ToolStarRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean exists(Long toolId, Long userId) {
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM tool_stars
                        WHERE tool_id = ?
                          AND user_id = ?
                        """,
                Integer.class,
                toolId,
                userId
        );
        return count != null && count > 0;
    }

    public void insert(Long toolId, Long userId) {
        jdbcTemplate.update("""
                        INSERT INTO tool_stars (tool_id, user_id)
                        VALUES (?, ?)
                        """,
                toolId,
                userId
        );
    }

    public void delete(Long toolId, Long userId) {
        jdbcTemplate.update("""
                        DELETE FROM tool_stars
                        WHERE tool_id = ?
                          AND user_id = ?
                        """,
                toolId,
                userId
        );
    }

    public void deleteByToolId(Long toolId) {
        jdbcTemplate.update("""
                        DELETE FROM tool_stars
                        WHERE tool_id = ?
                        """,
                toolId
        );
    }
}
