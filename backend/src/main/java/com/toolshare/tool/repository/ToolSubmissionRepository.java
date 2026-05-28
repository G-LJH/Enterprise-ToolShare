package com.toolshare.tool.repository;

import com.toolshare.review.model.ToolReviewRecord;
import com.toolshare.tool.model.ToolSubmissionRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;

@Repository
public class ToolSubmissionRepository {

    private static final RowMapper<ToolSubmissionRecord> TOOL_SUBMISSION_ROW_MAPPER = (rs, rowNum) -> new ToolSubmissionRecord(
            rs.getLong("id"),
            rs.getLong("tool_id"),
            rs.getLong("submitter_id"),
            rs.getString("submitter_name"),
            rs.getString("status"),
            rs.getString("remark"),
            rs.getBoolean("deleted"),
            rs.getObject("created_at", java.time.OffsetDateTime.class),
            rs.getObject("updated_at", java.time.OffsetDateTime.class)
    );

    private static final RowMapper<ToolReviewRecord> TOOL_REVIEW_ROW_MAPPER = (rs, rowNum) -> new ToolReviewRecord(
            rs.getLong("submission_id"),
            rs.getLong("tool_id"),
            rs.getLong("submitter_id"),
            rs.getString("submitter_name"),
            rs.getString("tool_name"),
            rs.getString("tool_summary"),
            rs.getString("tool_status"),
            rs.getString("submission_status"),
            rs.getString("remark"),
            rs.getObject("created_at", java.time.OffsetDateTime.class),
            rs.getObject("updated_at", java.time.OffsetDateTime.class)
    );

    private final JdbcTemplate jdbcTemplate;

    public ToolSubmissionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long insert(Long toolId, Long submitterId, String status, String remark, Long operatorId) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO tool_submissions (tool_id, submitter_id, status, remark, created_by, updated_by)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """, new String[]{"id"});
            statement.setLong(1, toolId);
            statement.setLong(2, submitterId);
            statement.setString(3, status);
            statement.setString(4, remark);
            statement.setLong(5, operatorId);
            statement.setLong(6, operatorId);
            return statement;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public Optional<ToolSubmissionRecord> findLatestByToolId(Long toolId) {
        List<ToolSubmissionRecord> submissions = jdbcTemplate.query("""
                        SELECT ts.id,
                               ts.tool_id,
                               ts.submitter_id,
                               u.real_name AS submitter_name,
                               ts.status,
                               ts.remark,
                               ts.deleted,
                               ts.created_at,
                               ts.updated_at
                        FROM tool_submissions ts
                        JOIN users u ON u.id = ts.submitter_id
                        WHERE ts.tool_id = ?
                          AND ts.deleted = FALSE
                        ORDER BY ts.created_at DESC, ts.id DESC
                        LIMIT 1
                        """,
                TOOL_SUBMISSION_ROW_MAPPER,
                toolId
        );
        return submissions.stream().findFirst();
    }

    public Optional<ToolSubmissionRecord> findById(Long submissionId) {
        List<ToolSubmissionRecord> submissions = jdbcTemplate.query("""
                        SELECT ts.id,
                               ts.tool_id,
                               ts.submitter_id,
                               u.real_name AS submitter_name,
                               ts.status,
                               ts.remark,
                               ts.deleted,
                               ts.created_at,
                               ts.updated_at
                        FROM tool_submissions ts
                        JOIN users u ON u.id = ts.submitter_id
                        WHERE ts.id = ?
                          AND ts.deleted = FALSE
                        """,
                TOOL_SUBMISSION_ROW_MAPPER,
                submissionId
        );
        return submissions.stream().findFirst();
    }

    public List<ToolReviewRecord> searchReviewRecords(String status) {
        return jdbcTemplate.query("""
                        SELECT ts.id AS submission_id,
                               ts.tool_id,
                               ts.submitter_id,
                               u.real_name AS submitter_name,
                               t.name AS tool_name,
                               t.summary AS tool_summary,
                               t.status AS tool_status,
                               ts.status AS submission_status,
                               ts.remark,
                               ts.created_at,
                               ts.updated_at
                        FROM tool_submissions ts
                        JOIN tools t ON t.id = ts.tool_id
                        JOIN users u ON u.id = ts.submitter_id
                        WHERE ts.deleted = FALSE
                          AND t.deleted = FALSE
                          AND ts.status = ?
                        ORDER BY ts.created_at ASC, ts.id ASC
                        """,
                TOOL_REVIEW_ROW_MAPPER,
                status
        );
    }

    public Optional<ToolReviewRecord> findReviewRecordById(Long submissionId) {
        List<ToolReviewRecord> records = jdbcTemplate.query("""
                        SELECT ts.id AS submission_id,
                               ts.tool_id,
                               ts.submitter_id,
                               u.real_name AS submitter_name,
                               t.name AS tool_name,
                               t.summary AS tool_summary,
                               t.status AS tool_status,
                               ts.status AS submission_status,
                               ts.remark,
                               ts.created_at,
                               ts.updated_at
                        FROM tool_submissions ts
                        JOIN tools t ON t.id = ts.tool_id
                        JOIN users u ON u.id = ts.submitter_id
                        WHERE ts.id = ?
                          AND ts.deleted = FALSE
                          AND t.deleted = FALSE
                        """,
                TOOL_REVIEW_ROW_MAPPER,
                submissionId
        );
        return records.stream().findFirst();
    }

    public void updateStatusAndRemark(Long submissionId, String status, String remark, Long operatorId) {
        jdbcTemplate.update("""
                        UPDATE tool_submissions
                        SET status = ?, remark = ?, updated_by = ?, updated_at = NOW()
                        WHERE id = ?
                          AND deleted = FALSE
                        """,
                status,
                remark,
                operatorId,
                submissionId
        );
    }

    public void deleteByToolId(Long toolId) {
        jdbcTemplate.update("""
                        DELETE FROM tool_submissions
                        WHERE tool_id = ?
                        """,
                toolId
        );
    }
}
