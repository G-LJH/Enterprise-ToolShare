package com.toolshare.importexport.repository;

import com.toolshare.importexport.model.ExportTaskRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;

@Repository
public class ExportTaskRepository {

    private static final RowMapper<ExportTaskRecord> EXPORT_TASK_ROW_MAPPER = (rs, rowNum) -> new ExportTaskRecord(
            rs.getLong("id"),
            rs.getString("task_name"),
            rs.getString("file_path"),
            rs.getString("file_name"),
            rs.getString("status"),
            rs.getLong("requester_id"),
            rs.getInt("success_count"),
            rs.getInt("failure_count"),
            rs.getString("error_report_path"),
            rs.getString("detail"),
            rs.getBoolean("deleted"),
            rs.getObject("created_at", java.time.OffsetDateTime.class),
            rs.getObject("updated_at", java.time.OffsetDateTime.class)
    );

    private final JdbcTemplate jdbcTemplate;

    public ExportTaskRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long insert(String taskName,
                       String filePath,
                       String fileName,
                       String status,
                       Long requesterId,
                       Integer successCount,
                       Integer failureCount,
                       String errorReportPath,
                       String detail,
                       Long operatorId) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO export_tasks (task_name, file_path, file_name, status, requester_id, success_count, failure_count, error_report_path, detail, created_by, updated_by)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, new String[]{"id"});
            statement.setString(1, taskName);
            statement.setString(2, filePath);
            statement.setString(3, fileName);
            statement.setString(4, status);
            statement.setLong(5, requesterId);
            statement.setInt(6, successCount);
            statement.setInt(7, failureCount);
            statement.setString(8, errorReportPath);
            statement.setString(9, detail);
            statement.setLong(10, operatorId);
            statement.setLong(11, operatorId);
            return statement;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public List<ExportTaskRecord> findAll() {
        return jdbcTemplate.query("""
                SELECT id, task_name, file_path, file_name, status, requester_id, success_count, failure_count, error_report_path, detail, deleted, created_at, updated_at
                FROM export_tasks
                WHERE deleted = FALSE
                ORDER BY created_at DESC, id DESC
                """, EXPORT_TASK_ROW_MAPPER);
    }
}
