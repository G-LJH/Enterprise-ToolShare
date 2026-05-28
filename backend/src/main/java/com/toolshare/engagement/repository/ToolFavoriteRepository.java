package com.toolshare.engagement.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ToolFavoriteRepository {

    private final JdbcTemplate jdbcTemplate;

    public ToolFavoriteRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean exists(Long toolId, Long userId) {
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM tool_favorites
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
                        INSERT INTO tool_favorites (tool_id, user_id)
                        VALUES (?, ?)
                        """,
                toolId,
                userId
        );
    }

    public void delete(Long toolId, Long userId) {
        jdbcTemplate.update("""
                        DELETE FROM tool_favorites
                        WHERE tool_id = ?
                          AND user_id = ?
                        """,
                toolId,
                userId
        );
    }

    public void deleteByToolId(Long toolId) {
        jdbcTemplate.update("""
                        DELETE FROM tool_favorites
                        WHERE tool_id = ?
                        """,
                toolId
        );
    }

    public List<Long> findFavoriteToolIdsByUserId(Long userId) {
        return jdbcTemplate.query("""
                        SELECT tool_id
                        FROM tool_favorites
                        WHERE user_id = ?
                        ORDER BY created_at DESC, id DESC
                        """,
                (rs, rowNum) -> rs.getLong("tool_id"),
                userId
        );
    }
}
