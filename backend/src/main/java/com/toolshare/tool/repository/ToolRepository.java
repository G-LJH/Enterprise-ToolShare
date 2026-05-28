package com.toolshare.tool.repository;

import com.toolshare.tool.model.ToolRecord;
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
public class ToolRepository {

    private static final RowMapper<ToolRecord> TOOL_ROW_MAPPER = (rs, rowNum) -> new ToolRecord(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("summary"),
            rs.getString("description"),
            rs.getString("url"),
            rs.getString("usage_guide"),
            rs.getLong("recommender_id"),
            rs.getString("recommender_name"),
            rs.getString("status"),
            rs.getInt("star_count"),
            rs.getInt("favorite_count"),
            rs.getInt("comment_count"),
            rs.getBoolean("deleted"),
            rs.getObject("created_at", java.time.OffsetDateTime.class),
            rs.getObject("updated_at", java.time.OffsetDateTime.class)
    );

    private final JdbcTemplate jdbcTemplate;

    public ToolRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long insert(String name,
                       String summary,
                       String description,
                       String url,
                       String usageGuide,
                       Long recommenderId,
                       String status,
                       Long operatorId) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO tools (name, summary, description, url, usage_guide, recommender_id, status, created_by, updated_by)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, new String[]{"id"});
            statement.setString(1, name);
            statement.setString(2, summary);
            statement.setString(3, description);
            statement.setString(4, url);
            statement.setString(5, usageGuide);
            statement.setLong(6, recommenderId);
            statement.setString(7, status);
            statement.setLong(8, operatorId);
            statement.setLong(9, operatorId);
            return statement;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public void updateTool(Long toolId,
                           String name,
                           String summary,
                           String description,
                           String url,
                           String usageGuide,
                           Long operatorId) {
        jdbcTemplate.update("""
                        UPDATE tools
                        SET name = ?, summary = ?, description = ?, url = ?, usage_guide = ?, updated_by = ?, updated_at = NOW()
                        WHERE id = ?
                          AND deleted = FALSE
                        """,
                name,
                summary,
                description,
                url,
                usageGuide,
                operatorId,
                toolId
        );
    }

    public void updateStatus(Long toolId, String status, Long operatorId) {
        jdbcTemplate.update("""
                        UPDATE tools
                        SET status = ?, updated_by = ?, updated_at = NOW()
                        WHERE id = ?
                          AND deleted = FALSE
                        """,
                status,
                operatorId,
                toolId
        );
    }

    public Optional<ToolRecord> findActiveById(Long toolId) {
        List<ToolRecord> tools = jdbcTemplate.query(baseSelect() + """
                        WHERE t.id = ?
                          AND t.deleted = FALSE
                        """,
                TOOL_ROW_MAPPER,
                toolId
        );
        return tools.stream().findFirst();
    }

    public List<ToolRecord> searchForAdmin(String keyword, String status, List<Long> tagIds, String sortBy, String sortOrder) {
        StringBuilder sql = new StringBuilder(baseSelect()).append("""
                WHERE t.deleted = FALSE
                """);
        List<Object> args = new ArrayList<>();
        appendKeywordFilter(sql, args, keyword);
        appendTagFilter(sql, args, tagIds);
        if (status != null && !status.isBlank()) {
            sql.append(" AND t.status = ?");
            args.add(status.trim().toUpperCase());
        }
        appendSort(sql, sortBy, sortOrder);
        return jdbcTemplate.query(sql.toString(), TOOL_ROW_MAPPER, args.toArray());
    }

    public List<ToolRecord> searchVisibleTools(String keyword, List<Long> tagIds, String sortBy, String sortOrder) {
        StringBuilder sql = new StringBuilder(baseSelect()).append("""
                WHERE t.deleted = FALSE
                  AND t.status = 'APPROVED'
                """);
        List<Object> args = new ArrayList<>();
        appendKeywordFilter(sql, args, keyword);
        appendTagFilter(sql, args, tagIds);
        appendSort(sql, sortBy, sortOrder);
        return jdbcTemplate.query(sql.toString(), TOOL_ROW_MAPPER, args.toArray());
    }

    public List<ToolRecord> searchByRecommender(Long recommenderId, String status, List<Long> tagIds, String sortBy, String sortOrder) {
        StringBuilder sql = new StringBuilder(baseSelect()).append("""
                WHERE t.deleted = FALSE
                  AND t.recommender_id = ?
                """);
        List<Object> args = new ArrayList<>();
        args.add(recommenderId);
        appendTagFilter(sql, args, tagIds);
        if (status != null && !status.isBlank()) {
            sql.append(" AND t.status = ?");
            args.add(status.trim().toUpperCase());
        }
        appendSort(sql, sortBy, sortOrder);
        return jdbcTemplate.query(sql.toString(), TOOL_ROW_MAPPER, args.toArray());
    }

    public Optional<ToolRecord> findDuplicateCandidate(String normalizedName, String normalizedUrl, Long excludedToolId) {
        StringBuilder sql = new StringBuilder(baseSelect()).append("""
                WHERE t.deleted = FALSE
                  AND t.status <> 'REJECTED'
                  AND (
                    LOWER(t.name) = ?
                """);
        List<Object> args = new ArrayList<>();
        args.add(normalizedName);
        if (normalizedUrl != null && !normalizedUrl.isBlank()) {
            sql.append(" OR LOWER(t.url) = ?");
            args.add(normalizedUrl);
        }
        sql.append(")");
        if (excludedToolId != null) {
            sql.append(" AND t.id <> ?");
            args.add(excludedToolId);
        }
        List<ToolRecord> tools = jdbcTemplate.query(sql.toString(), TOOL_ROW_MAPPER, args.toArray());
        return tools.stream().findFirst();
    }

    public List<ToolRecord> findByIds(List<Long> toolIds) {
        if (toolIds.isEmpty()) {
            return List.of();
        }
        String placeholders = toolIds.stream().map(id -> "?").reduce((left, right) -> left + "," + right).orElse("?");
        return jdbcTemplate.query(baseSelect() + """
                        WHERE t.deleted = FALSE
                          AND t.id IN (%s)
                        ORDER BY t.updated_at DESC, t.id DESC
                        """.formatted(placeholders),
                TOOL_ROW_MAPPER,
                toolIds.toArray()
        );
    }

    public void physicalDelete(Long toolId) {
        jdbcTemplate.update("""
                        DELETE FROM tools
                        WHERE id = ?
                          AND deleted = FALSE
                        """,
                toolId
        );
    }

    public void incrementStarCount(Long toolId) {
        jdbcTemplate.update("""
                        UPDATE tools
                        SET star_count = star_count + 1, updated_at = NOW()
                        WHERE id = ?
                          AND deleted = FALSE
                        """,
                toolId
        );
    }

    public void decrementStarCount(Long toolId) {
        jdbcTemplate.update("""
                        UPDATE tools
                        SET star_count = CASE WHEN star_count > 0 THEN star_count - 1 ELSE 0 END, updated_at = NOW()
                        WHERE id = ?
                          AND deleted = FALSE
                        """,
                toolId
        );
    }

    public void incrementFavoriteCount(Long toolId) {
        jdbcTemplate.update("""
                        UPDATE tools
                        SET favorite_count = favorite_count + 1, updated_at = NOW()
                        WHERE id = ?
                          AND deleted = FALSE
                        """,
                toolId
        );
    }

    public void decrementFavoriteCount(Long toolId) {
        jdbcTemplate.update("""
                        UPDATE tools
                        SET favorite_count = CASE WHEN favorite_count > 0 THEN favorite_count - 1 ELSE 0 END, updated_at = NOW()
                        WHERE id = ?
                          AND deleted = FALSE
                        """,
                toolId
        );
    }

    public void incrementCommentCount(Long toolId) {
        jdbcTemplate.update("""
                        UPDATE tools
                        SET comment_count = comment_count + 1, updated_at = NOW()
                        WHERE id = ?
                          AND deleted = FALSE
                        """,
                toolId
        );
    }

    public void decrementCommentCount(Long toolId) {
        jdbcTemplate.update("""
                        UPDATE tools
                        SET comment_count = CASE WHEN comment_count > 0 THEN comment_count - 1 ELSE 0 END, updated_at = NOW()
                        WHERE id = ?
                          AND deleted = FALSE
                        """,
                toolId
        );
    }

    private void appendKeywordFilter(StringBuilder sql, List<Object> args, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return;
        }
        sql.append(" AND (LOWER(t.name) LIKE ? OR LOWER(COALESCE(t.summary, '')) LIKE ? OR LOWER(COALESCE(t.description, '')) LIKE ?)");
        String like = "%" + keyword.trim().toLowerCase() + "%";
        args.add(like);
        args.add(like);
        args.add(like);
    }

    private void appendTagFilter(StringBuilder sql, List<Object> args, List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        String placeholders = tagIds.stream().map(id -> "?").reduce((left, right) -> left + "," + right).orElse("?");
        sql.append("""
                 AND EXISTS (
                    SELECT 1
                    FROM tool_tags tt
                    JOIN tags tg ON tg.id = tt.tag_id
                    WHERE tt.tool_id = t.id
                      AND tg.deleted = FALSE
                      AND tt.tag_id IN (%s)
                 )
                """.formatted(placeholders));
        args.addAll(tagIds);
    }

    private void appendSort(StringBuilder sql, String sortBy, String sortOrder) {
        String column = "createdAt".equals(sortBy) ? "t.created_at" : "t.star_count";
        String order = "asc".equalsIgnoreCase(sortOrder) ? "ASC" : "DESC";
        sql.append(" ORDER BY ").append(column).append(" ").append(order).append(", t.id DESC");
    }

    private String baseSelect() {
        return """
                SELECT t.id,
                       t.name,
                       t.summary,
                       t.description,
                       t.url,
                       t.usage_guide,
                       t.recommender_id,
                       u.real_name AS recommender_name,
                       t.status,
                       t.star_count,
                       t.favorite_count,
                       t.comment_count,
                       t.deleted,
                       t.created_at,
                       t.updated_at
                FROM tools t
                LEFT JOIN users u ON u.id = t.recommender_id
                """;
    }
}
