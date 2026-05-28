package com.toolshare.workflow.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class WorkflowFavoriteRepository {

    private final JdbcTemplate jdbcTemplate;

    public WorkflowFavoriteRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean exists(Long workflowId, Long userId) {
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM workflow_favorites
                        WHERE workflow_id = ?
                          AND user_id = ?
                        """,
                Integer.class,
                workflowId,
                userId
        );
        return count != null && count > 0;
    }

    public void insert(Long workflowId, Long userId) {
        jdbcTemplate.update("""
                        INSERT INTO workflow_favorites (workflow_id, user_id)
                        VALUES (?, ?)
                        """,
                workflowId,
                userId
        );
    }

    public void delete(Long workflowId, Long userId) {
        jdbcTemplate.update("""
                        DELETE FROM workflow_favorites
                        WHERE workflow_id = ?
                          AND user_id = ?
                        """,
                workflowId,
                userId
        );
    }

    public List<Long> findFavoriteWorkflowIdsByUserId(Long userId) {
        return jdbcTemplate.queryForList("""
                        SELECT workflow_id
                        FROM workflow_favorites
                        WHERE user_id = ?
                        ORDER BY created_at DESC
                        """,
                Long.class,
                userId
        );
    }
}
