package com.toolshare.workflow.repository;

import com.toolshare.workflow.model.WorkflowRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class WorkflowRepository {

    private static final RowMapper<WorkflowRecord> WORKFLOW_ROW_MAPPER = (rs, rowNum) -> new WorkflowRecord(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("scenario"),
            rs.getString("description"),
            rs.getString("steps"),
            rs.getBoolean("featured"),
            rs.getString("status"),
            rs.getLong("creator_id"),
            rs.getString("creator_name"),
            rs.getInt("star_count"),
            rs.getInt("favorite_count"),
            rs.getBoolean("deleted"),
            rs.getObject("created_at", java.time.OffsetDateTime.class),
            rs.getObject("updated_at", java.time.OffsetDateTime.class)
    );

    private final JdbcTemplate jdbcTemplate;

    public WorkflowRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long insert(String name,
                       String scenario,
                       String description,
                       String steps,
                       boolean featured,
                       Long creatorId,
                       Long operatorId) {
        return insert(name, scenario, description, steps, featured, "ACTIVE", creatorId, operatorId);
    }

    public long insert(String name,
                       String scenario,
                       String description,
                       String steps,
                       boolean featured,
                       String status,
                       Long creatorId,
                       Long operatorId) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO workflows (name, scenario, description, steps, featured, status, creator_id, created_by, updated_by)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, new String[]{"id"});
            statement.setString(1, name);
            statement.setString(2, scenario);
            statement.setString(3, description);
            statement.setString(4, steps);
            statement.setBoolean(5, featured);
            statement.setString(6, status);
            statement.setLong(7, creatorId);
            statement.setLong(8, operatorId);
            statement.setLong(9, operatorId);
            return statement;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public void updateWorkflow(Long workflowId,
                               String name,
                               String scenario,
                               String description,
                               String steps,
                               boolean featured,
                               Long operatorId) {
        jdbcTemplate.update("""
                        UPDATE workflows
                        SET name = ?, scenario = ?, description = ?, steps = ?, featured = ?, updated_by = ?, updated_at = NOW()
                        WHERE id = ?
                          AND deleted = FALSE
                        """,
                name,
                scenario,
                description,
                steps,
                featured,
                operatorId,
                workflowId
        );
    }

    public void softDelete(Long workflowId, Long operatorId) {
        jdbcTemplate.update("""
                        UPDATE workflows
                        SET deleted = TRUE, updated_by = ?, updated_at = NOW()
                        WHERE id = ?
                          AND deleted = FALSE
                        """,
                operatorId,
                workflowId
        );
    }

    public Optional<WorkflowRecord> findActiveById(Long workflowId) {
        List<WorkflowRecord> records = jdbcTemplate.query(baseSelect() + """
                        WHERE w.id = ?
                          AND w.deleted = FALSE
                        """,
                WORKFLOW_ROW_MAPPER,
                workflowId
        );
        return records.stream().findFirst();
    }

    public List<WorkflowRecord> searchVisible(String scenario, Boolean featuredOnly) {
        StringBuilder sql = new StringBuilder(baseSelect()).append("""
                WHERE w.deleted = FALSE
                  AND w.status = 'ACTIVE'
                """);
        List<Object> args = new ArrayList<>();
        appendScenarioFilter(sql, args, scenario);
        if (Boolean.TRUE.equals(featuredOnly)) {
            sql.append(" AND w.featured = TRUE");
        }
        sql.append(" ORDER BY w.featured DESC, w.updated_at DESC, w.id DESC");
        return jdbcTemplate.query(sql.toString(), WORKFLOW_ROW_MAPPER, args.toArray());
    }

    public List<WorkflowRecord> searchForAdmin(String scenario) {
        StringBuilder sql = new StringBuilder(baseSelect()).append("""
                WHERE w.deleted = FALSE
                """);
        List<Object> args = new ArrayList<>();
        appendScenarioFilter(sql, args, scenario);
        sql.append(" ORDER BY w.updated_at DESC, w.id DESC");
        return jdbcTemplate.query(sql.toString(), WORKFLOW_ROW_MAPPER, args.toArray());
    }

    public List<WorkflowRecord> findByIds(List<Long> workflowIds) {
        if (workflowIds == null || workflowIds.isEmpty()) {
            return List.of();
        }
        String placeholders = workflowIds.stream().map(id -> "?").collect(java.util.stream.Collectors.joining(","));
        String sql = baseSelect() + " WHERE w.id IN (" + placeholders + ") AND w.deleted = FALSE ORDER BY w.updated_at DESC, w.id DESC";
        return jdbcTemplate.query(sql, WORKFLOW_ROW_MAPPER, workflowIds.toArray());
    }

    private void appendScenarioFilter(StringBuilder sql, List<Object> args, String scenario) {
        if (scenario == null || scenario.isBlank()) {
            return;
        }
        sql.append(" AND LOWER(w.scenario) LIKE ?");
        args.add("%" + scenario.trim().toLowerCase() + "%");
    }

    private String baseSelect() {
        return """
                SELECT w.id,
                       w.name,
                       w.scenario,
                       w.description,
                       w.steps,
                       w.featured,
                       w.status,
                       w.creator_id,
                       u.real_name AS creator_name,
                       w.star_count,
                       w.favorite_count,
                       w.deleted,
                       w.created_at,
                       w.updated_at
                FROM workflows w
                JOIN users u ON u.id = w.creator_id
                """;
    }

    public void incrementStarCount(Long workflowId) {
        jdbcTemplate.update("""
                        UPDATE workflows
                        SET star_count = star_count + 1, updated_at = NOW()
                        WHERE id = ?
                          AND deleted = FALSE
                        """,
                workflowId
        );
    }

    public void decrementStarCount(Long workflowId) {
        jdbcTemplate.update("""
                        UPDATE workflows
                        SET star_count = GREATEST(star_count - 1, 0), updated_at = NOW()
                        WHERE id = ?
                          AND deleted = FALSE
                        """,
                workflowId
        );
    }

    public void incrementFavoriteCount(Long workflowId) {
        jdbcTemplate.update("""
                        UPDATE workflows
                        SET favorite_count = favorite_count + 1, updated_at = NOW()
                        WHERE id = ?
                          AND deleted = FALSE
                        """,
                workflowId
        );
    }

    public void decrementFavoriteCount(Long workflowId) {
        jdbcTemplate.update("""
                        UPDATE workflows
                        SET favorite_count = GREATEST(favorite_count - 1, 0), updated_at = NOW()
                        WHERE id = ?
                          AND deleted = FALSE
                        """,
                workflowId
        );
    }

    public void updateStatus(Long workflowId, String status, Long operatorId) {
        jdbcTemplate.update("""
                        UPDATE workflows
                        SET status = ?, updated_by = ?, updated_at = NOW()
                        WHERE id = ?
                        """,
                status,
                operatorId,
                workflowId
        );
    }
}
