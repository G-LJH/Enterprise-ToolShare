package com.toolshare.engagement.repository;

import com.toolshare.engagement.model.ToolCommentRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;

@Repository
public class ToolCommentRepository {

    private static final RowMapper<ToolCommentRecord> COMMENT_ROW_MAPPER = (rs, rowNum) -> new ToolCommentRecord(
            rs.getLong("id"),
            rs.getLong("tool_id"),
            rs.getLong("user_id"),
            rs.getString("author_name"),
            rs.getString("content"),
            rs.getString("status"),
            rs.getBoolean("deleted"),
            rs.getObject("created_at", java.time.OffsetDateTime.class),
            rs.getObject("updated_at", java.time.OffsetDateTime.class)
    );

    private final JdbcTemplate jdbcTemplate;

    public ToolCommentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long insert(Long toolId, Long userId, String content, Long operatorId) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO tool_comments (tool_id, user_id, parent_comment_id, content, status, created_by, updated_by)
                    VALUES (?, ?, NULL, ?, 'VISIBLE', ?, ?)
                    """, new String[]{"id"});
            statement.setLong(1, toolId);
            statement.setLong(2, userId);
            statement.setString(3, content);
            statement.setLong(4, operatorId);
            statement.setLong(5, operatorId);
            return statement;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public List<ToolCommentRecord> findVisibleByToolId(Long toolId) {
        return jdbcTemplate.query("""
                        SELECT tc.id,
                               tc.tool_id,
                               tc.user_id,
                               u.real_name AS author_name,
                               tc.content,
                               tc.status,
                               tc.deleted,
                               tc.created_at,
                               tc.updated_at
                        FROM tool_comments tc
                        JOIN users u ON u.id = tc.user_id
                        WHERE tc.tool_id = ?
                          AND tc.deleted = FALSE
                          AND tc.status = 'VISIBLE'
                        ORDER BY tc.created_at ASC, tc.id ASC
                        """,
                COMMENT_ROW_MAPPER,
                toolId
        );
    }

    public Optional<ToolCommentRecord> findActiveById(Long commentId) {
        List<ToolCommentRecord> comments = jdbcTemplate.query("""
                        SELECT tc.id,
                               tc.tool_id,
                               tc.user_id,
                               u.real_name AS author_name,
                               tc.content,
                               tc.status,
                               tc.deleted,
                               tc.created_at,
                               tc.updated_at
                        FROM tool_comments tc
                        JOIN users u ON u.id = tc.user_id
                        WHERE tc.id = ?
                          AND tc.deleted = FALSE
                        """,
                COMMENT_ROW_MAPPER,
                commentId
        );
        return comments.stream().findFirst();
    }

    public void logicalDelete(Long commentId, Long operatorId) {
        jdbcTemplate.update("""
                        UPDATE tool_comments
                        SET deleted = TRUE, status = 'DELETED', updated_by = ?, updated_at = NOW()
                        WHERE id = ?
                          AND deleted = FALSE
                        """,
                operatorId,
                commentId
        );
    }
}
