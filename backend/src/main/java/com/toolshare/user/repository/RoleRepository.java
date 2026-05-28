package com.toolshare.user.repository;

import com.toolshare.user.model.RoleRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class RoleRepository {

    public static final String ADMIN = "ADMIN";
    public static final String USER = "USER";
    public static final String REVIEWER = "REVIEWER";

    private static final RowMapper<RoleRecord> ROLE_ROW_MAPPER = (rs, rowNum) -> new RoleRecord(
            rs.getLong("id"),
            rs.getString("code"),
            rs.getString("name"),
            rs.getString("description")
    );

    private final JdbcTemplate jdbcTemplate;

    public RoleRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<RoleRecord> findAllActive() {
        return jdbcTemplate.query("""
                SELECT id, code, name, description
                FROM roles
                WHERE deleted = FALSE
                ORDER BY id
                """, ROLE_ROW_MAPPER);
    }

    public List<RoleRecord> findByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        String placeholders = ids.stream().map(id -> "?").reduce((left, right) -> left + "," + right).orElse("?");
        return jdbcTemplate.query("""
                        SELECT id, code, name, description
                        FROM roles
                        WHERE deleted = FALSE
                          AND id IN (%s)
                        ORDER BY id
                        """.formatted(placeholders),
                ROLE_ROW_MAPPER,
                ids.toArray()
        );
    }

    public Optional<RoleRecord> findByCode(String code) {
        List<RoleRecord> roles = jdbcTemplate.query("""
                        SELECT id, code, name, description
                        FROM roles
                        WHERE deleted = FALSE
                          AND code = ?
                        """,
                ROLE_ROW_MAPPER,
                code
        );
        return roles.stream().findFirst();
    }

    public long insert(String code, String name, String description) {
        jdbcTemplate.update("""
                        INSERT INTO roles (code, name, description, created_by, updated_by)
                        VALUES (?, ?, ?, 0, 0)
                        """,
                code,
                name,
                description
        );
        return jdbcTemplate.queryForObject("SELECT id FROM roles WHERE code = ?", Long.class, code);
    }
}
