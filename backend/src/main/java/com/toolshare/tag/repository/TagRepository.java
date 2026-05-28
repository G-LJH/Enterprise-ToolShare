package com.toolshare.tag.repository;

import com.toolshare.tag.model.TagRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class TagRepository {

    private static final RowMapper<TagRecord> TAG_ROW_MAPPER = (rs, rowNum) -> new TagRecord(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("description"),
            rs.getBoolean("deleted"),
            rs.getObject("created_at", java.time.OffsetDateTime.class),
            rs.getObject("updated_at", java.time.OffsetDateTime.class)
    );

    private final JdbcTemplate jdbcTemplate;

    public TagRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<TagRecord> findAllActive() {
        return jdbcTemplate.query("""
                SELECT id, name, description, deleted, created_at, updated_at
                FROM tags
                WHERE deleted = FALSE
                ORDER BY name ASC, id ASC
                """, TAG_ROW_MAPPER);
    }

    public Optional<TagRecord> findActiveById(Long tagId) {
        List<TagRecord> tags = jdbcTemplate.query("""
                        SELECT id, name, description, deleted, created_at, updated_at
                        FROM tags
                        WHERE id = ?
                          AND deleted = FALSE
                        """,
                TAG_ROW_MAPPER,
                tagId
        );
        return tags.stream().findFirst();
    }

    public Optional<TagRecord> findByName(String normalizedName) {
        List<TagRecord> tags = jdbcTemplate.query("""
                        SELECT id, name, description, deleted, created_at, updated_at
                        FROM tags
                        WHERE LOWER(name) = ?
                          AND deleted = FALSE
                        """,
                TAG_ROW_MAPPER,
                normalizedName
        );
        return tags.stream().findFirst();
    }

    public List<TagRecord> findByIds(List<Long> tagIds) {
        if (tagIds.isEmpty()) {
            return List.of();
        }
        String placeholders = tagIds.stream().map(id -> "?").reduce((left, right) -> left + "," + right).orElse("?");
        return jdbcTemplate.query("""
                        SELECT id, name, description, deleted, created_at, updated_at
                        FROM tags
                        WHERE deleted = FALSE
                          AND id IN (%s)
                        ORDER BY name ASC, id ASC
                        """.formatted(placeholders),
                TAG_ROW_MAPPER,
                tagIds.toArray()
        );
    }

    public long insert(String name, String description, Long operatorId) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO tags (name, description, created_by, updated_by)
                    VALUES (?, ?, ?, ?)
                    """, new String[]{"id"});
            statement.setString(1, name);
            statement.setString(2, description);
            statement.setLong(3, operatorId);
            statement.setLong(4, operatorId);
            return statement;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public void updateTag(Long tagId, String name, String description, Long operatorId) {
        jdbcTemplate.update("""
                        UPDATE tags
                        SET name = ?, description = ?, updated_by = ?, updated_at = NOW()
                        WHERE id = ?
                          AND deleted = FALSE
                        """,
                name,
                description,
                operatorId,
                tagId
        );
    }

    public void logicalDelete(Long tagId, Long operatorId) {
        jdbcTemplate.update("""
                        UPDATE tags
                        SET deleted = TRUE, updated_by = ?, updated_at = NOW()
                        WHERE id = ?
                          AND deleted = FALSE
                        """,
                operatorId,
                tagId
        );
    }

    public Map<Long, List<TagRecord>> findByToolIds(List<Long> toolIds) {
        Map<Long, List<TagRecord>> tagsByToolId = new LinkedHashMap<>();
        if (toolIds.isEmpty()) {
            return tagsByToolId;
        }
        String placeholders = toolIds.stream().map(id -> "?").reduce((left, right) -> left + "," + right).orElse("?");
        jdbcTemplate.query("""
                        SELECT tt.tool_id, t.id, t.name, t.description, t.deleted, t.created_at, t.updated_at
                        FROM tool_tags tt
                        JOIN tags t ON t.id = tt.tag_id
                        WHERE tt.tool_id IN (%s)
                          AND t.deleted = FALSE
                        ORDER BY tt.tool_id ASC, t.name ASC, t.id ASC
                        """.formatted(placeholders),
                rs -> {
                    Long toolId = rs.getLong("tool_id");
                    tagsByToolId.computeIfAbsent(toolId, key -> new ArrayList<>())
                            .add(new TagRecord(
                                    rs.getLong("id"),
                                    rs.getString("name"),
                                    rs.getString("description"),
                                    rs.getBoolean("deleted"),
                                    rs.getObject("created_at", java.time.OffsetDateTime.class),
                                    rs.getObject("updated_at", java.time.OffsetDateTime.class)
                            ));
                },
                toolIds.toArray()
        );
        return tagsByToolId;
    }
}
