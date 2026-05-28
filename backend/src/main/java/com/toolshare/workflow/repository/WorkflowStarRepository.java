package com.toolshare.workflow.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class WorkflowStarRepository {

    private final JdbcTemplate jdbcTemplate;

    public WorkflowStarRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean exists(Long workflowId, Long userId) {
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM workflow_stars
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
                        INSERT INTO workflow_stars (workflow_id, user_id)
                        VALUES (?, ?)
                        """,
                workflowId,
                userId
        );
    }

    public void delete(Long workflowId, Long userId) {
        jdbcTemplate.update("""
                        DELETE FROM workflow_stars
                        WHERE workflow_id = ?
                          AND user_id = ?
                        """,
                workflowId,
                userId
        );
    }
}
