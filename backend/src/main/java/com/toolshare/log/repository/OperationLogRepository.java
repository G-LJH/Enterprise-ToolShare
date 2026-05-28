package com.toolshare.log.repository;

import com.toolshare.log.model.OperationLogRecord;
import com.toolshare.model.OperationLogEntry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
public class OperationLogRepository {

    private static final RowMapper<OperationLogRecord> ROW_MAPPER = new RowMapper<>() {
        @Override
        public OperationLogRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new OperationLogRecord(
                    rs.getLong("id"),
                    rs.getString("operation"),
                    rs.getString("module"),
                    rs.getObject("user_id", Long.class),
                    rs.getString("username"),
                    rs.getString("real_name"),
                    rs.getBoolean("success"),
                    rs.getString("message"),
                    rs.getObject("created_at", java.time.OffsetDateTime.class)
            );
        }
    };

    private final JdbcTemplate jdbcTemplate;

    public OperationLogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(OperationLogEntry entry) {
        jdbcTemplate.update("""
                        INSERT INTO operation_logs (operation, module, user_id, success, message)
                        VALUES (?, ?, ?, ?, ?)
                        """,
                entry.operation(),
                entry.module(),
                entry.userId(),
                entry.success(),
                entry.message()
        );
    }

    public void cleanupOldLogs(int retentionLimit) {
        jdbcTemplate.update("""
                        DELETE FROM operation_logs
                        WHERE id IN (
                            SELECT id
                            FROM operation_logs
                            ORDER BY created_at DESC, id DESC
                            OFFSET ?
                        )
                        """,
                retentionLimit);
    }

    public List<OperationLogRecord> search(String module, Boolean success, String keyword, int limit, int offset) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT ol.id,
                       ol.operation,
                       ol.module,
                       ol.user_id,
                       u.username,
                       u.real_name,
                       ol.success,
                       ol.message,
                       ol.created_at
                FROM operation_logs ol
                LEFT JOIN users u ON u.id = ol.user_id
                WHERE 1 = 1
                """);
        appendFilters(sql, args, module, success, keyword);
        sql.append("""
                ORDER BY ol.created_at DESC, ol.id DESC
                LIMIT ?
                OFFSET ?
                """);
        args.add(limit);
        args.add(offset);
        return jdbcTemplate.query(sql.toString(), ROW_MAPPER, args.toArray());
    }

    public long count(String module, Boolean success, String keyword) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(*)
                FROM operation_logs ol
                LEFT JOIN users u ON u.id = ol.user_id
                WHERE 1 = 1
                """);
        appendFilters(sql, args, module, success, keyword);
        return jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
    }

    private void appendFilters(StringBuilder sql, List<Object> args, String module, Boolean success, String keyword) {
        if (module != null && !module.isBlank()) {
            sql.append(" AND ol.module = ?");
            args.add(module.trim());
        }
        if (success != null) {
            sql.append(" AND ol.success = ?");
            args.add(success);
        }
        if (keyword != null && !keyword.isBlank()) {
            sql.append("""
                     AND (
                        LOWER(ol.operation) LIKE ?
                        OR LOWER(COALESCE(ol.message, '')) LIKE ?
                        OR LOWER(COALESCE(u.username, '')) LIKE ?
                        OR LOWER(COALESCE(u.real_name, '')) LIKE ?
                     )
                    """);
            String likeValue = "%" + keyword.trim().toLowerCase() + "%";
            args.add(likeValue);
            args.add(likeValue);
            args.add(likeValue);
            args.add(likeValue);
        }
    }
}
