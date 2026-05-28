package com.toolshare.user.repository;

import com.toolshare.log.model.AuditLogRecord;
import com.toolshare.model.AuditLogEntry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
public class AuditLogRepository {

    private static final RowMapper<AuditLogRecord> ROW_MAPPER = new RowMapper<>() {
        @Override
        public AuditLogRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new AuditLogRecord(
                    rs.getLong("id"),
                    rs.getString("action"),
                    rs.getString("object_type"),
                    rs.getString("object_id"),
                    rs.getObject("operator_id", Long.class),
                    rs.getString("operator_name"),
                    rs.getString("detail"),
                    rs.getObject("created_at", java.time.OffsetDateTime.class)
            );
        }
    };

    private final JdbcTemplate jdbcTemplate;

    public AuditLogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(AuditLogEntry entry) {
        jdbcTemplate.update("""
                        INSERT INTO audit_logs (action, object_type, object_id, operator_id, operator_name, detail)
                        VALUES (?, ?, ?, ?, ?, ?)
                        """,
                entry.action(),
                entry.objectType(),
                entry.objectId(),
                entry.operatorId(),
                entry.operatorName(),
                entry.detail()
        );
    }

    public List<AuditLogRecord> search(String action, String objectType, String keyword, int limit, int offset) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT id, action, object_type, object_id, operator_id, operator_name, detail, created_at
                FROM audit_logs
                WHERE 1 = 1
                """);
        appendFilters(sql, args, action, objectType, keyword);
        sql.append("""
                ORDER BY created_at DESC, id DESC
                LIMIT ?
                OFFSET ?
                """);
        args.add(limit);
        args.add(offset);
        return jdbcTemplate.query(sql.toString(), ROW_MAPPER, args.toArray());
    }

    public long count(String action, String objectType, String keyword) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(*)
                FROM audit_logs
                WHERE 1 = 1
                """);
        appendFilters(sql, args, action, objectType, keyword);
        return jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
    }

    private void appendFilters(StringBuilder sql, List<Object> args, String action, String objectType, String keyword) {
        if (action != null && !action.isBlank()) {
            sql.append(" AND action = ?");
            args.add(action.trim());
        }
        if (objectType != null && !objectType.isBlank()) {
            sql.append(" AND object_type = ?");
            args.add(objectType.trim());
        }
        if (keyword != null && !keyword.isBlank()) {
            sql.append("""
                     AND (
                        LOWER(COALESCE(operator_name, '')) LIKE ?
                        OR LOWER(COALESCE(object_id, '')) LIKE ?
                        OR LOWER(COALESCE(detail, '')) LIKE ?
                     )
                    """);
            String likeValue = "%" + keyword.trim().toLowerCase() + "%";
            args.add(likeValue);
            args.add(likeValue);
            args.add(likeValue);
        }
    }
}
