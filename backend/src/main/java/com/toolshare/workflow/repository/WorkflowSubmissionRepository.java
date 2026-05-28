package com.toolshare.workflow.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;

@Repository
public class WorkflowSubmissionRepository {

    public record WorkflowSubmissionRecord(
            Long id,
            Long workflowId,
            Long submitterId,
            String status,
            String remark,
            Long workflowCreatorId,
            String workflowName,
            String workflowScenario,
            String workflowDescription,
            String workflowSteps,
            Boolean workflowFeatured,
            Integer workflowStarCount,
            Integer workflowFavoriteCount
    ) {
    }

    private static final RowMapper<WorkflowSubmissionRecord> SUBMISSION_ROW_MAPPER = (rs, rowNum) -> new WorkflowSubmissionRecord(
            rs.getLong("id"),
            rs.getLong("workflow_id"),
            rs.getLong("submitter_id"),
            rs.getString("submission_status"),
            rs.getString("remark"),
            rs.getLong("creator_id"),
            rs.getString("name"),
            rs.getString("scenario"),
            rs.getString("description"),
            rs.getString("steps"),
            rs.getBoolean("featured"),
            rs.getInt("star_count"),
            rs.getInt("favorite_count")
    );

    private final JdbcTemplate jdbcTemplate;

    public WorkflowSubmissionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long insert(Long workflowId, Long submitterId, String status, String remark, Long operatorId) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO workflow_submissions (workflow_id, submitter_id, status, remark, created_by, updated_by)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """, new String[]{"id"});
            statement.setLong(1, workflowId);
            statement.setLong(2, submitterId);
            statement.setString(3, status);
            statement.setString(4, remark);
            statement.setLong(5, operatorId);
            statement.setLong(6, operatorId);
            return statement;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public void updateStatusAndRemark(Long submissionId, String status, String remark, Long operatorId) {
        jdbcTemplate.update("""
                        UPDATE workflow_submissions
                        SET status = ?, remark = ?, updated_by = ?, updated_at = NOW()
                        WHERE id = ?
                        """,
                status,
                remark,
                operatorId,
                submissionId
        );
    }

    public Optional<WorkflowSubmissionRecord> findById(Long submissionId) {
        List<WorkflowSubmissionRecord> records = jdbcTemplate.query("""
                        SELECT ws.id,
                               ws.workflow_id,
                               ws.submitter_id,
                               ws.status AS submission_status,
                               ws.remark,
                               w.creator_id,
                               w.name,
                               w.scenario,
                               w.description,
                               w.steps,
                               w.featured,
                               w.star_count,
                               w.favorite_count
                        FROM workflow_submissions ws
                        JOIN workflows w ON w.id = ws.workflow_id
                        WHERE ws.id = ?
                          AND ws.deleted = FALSE
                        """,
                SUBMISSION_ROW_MAPPER,
                submissionId
        );
        return records.stream().findFirst();
    }

    public Optional<WorkflowSubmissionRecord> findLatestByWorkflowId(Long workflowId) {
        List<WorkflowSubmissionRecord> records = jdbcTemplate.query("""
                        SELECT ws.id,
                               ws.workflow_id,
                               ws.submitter_id,
                               ws.status AS submission_status,
                               ws.remark,
                               w.creator_id,
                               w.name,
                               w.scenario,
                               w.description,
                               w.steps,
                               w.featured,
                               w.star_count,
                               w.favorite_count
                        FROM workflow_submissions ws
                        JOIN workflows w ON w.id = ws.workflow_id
                        WHERE ws.workflow_id = ?
                          AND ws.deleted = FALSE
                        ORDER BY ws.created_at DESC
                        LIMIT 1
                        """,
                SUBMISSION_ROW_MAPPER,
                workflowId
        );
        return records.stream().findFirst();
    }

    public List<WorkflowSubmissionRecord> searchReviewRecords(String status) {
        StringBuilder sql = new StringBuilder("""
                SELECT ws.id,
                       ws.workflow_id,
                       ws.submitter_id,
                       ws.status AS submission_status,
                       ws.remark,
                       w.creator_id,
                       w.name,
                       w.scenario,
                       w.description,
                       w.steps,
                       w.featured,
                       w.star_count,
                       w.favorite_count
                FROM workflow_submissions ws
                JOIN workflows w ON w.id = ws.workflow_id
                WHERE ws.deleted = FALSE
                """);
        if (status != null && !status.isBlank()) {
            sql.append(" AND ws.status = ?");
            return jdbcTemplate.query(sql.toString(), SUBMISSION_ROW_MAPPER, status);
        }
        return jdbcTemplate.query(sql.toString(), SUBMISSION_ROW_MAPPER);
    }
}
