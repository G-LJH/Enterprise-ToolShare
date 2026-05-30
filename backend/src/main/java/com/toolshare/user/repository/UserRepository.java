package com.toolshare.user.repository;

import com.toolshare.user.model.RoleRecord;
import com.toolshare.user.model.UserRecord;
import com.toolshare.user.model.UserRoleMapping;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Types;
import java.sql.Statement;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class UserRepository {

    private static final RowMapper<UserRecord> USER_ROW_MAPPER = (rs, rowNum) -> new UserRecord(
            rs.getLong("id"),
            rs.getString("user_code"),
            rs.getString("username"),
            rs.getString("email"),
            rs.getString("real_name"),
            rs.getString("nickname"),
            rs.getString("status"),
            rs.getString("password_hash"),
            rs.getInt("login_fail_count"),
            rs.getObject("last_login_at", java.time.OffsetDateTime.class),
            rs.getObject("login_locked_until", java.time.OffsetDateTime.class),
            rs.getBoolean("deleted"),
            rs.getObject("created_at", java.time.OffsetDateTime.class),
            rs.getObject("updated_at", java.time.OffsetDateTime.class)
    );

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<UserRecord> findById(Long id) {
        List<UserRecord> users = jdbcTemplate.query("""
                        SELECT id, user_code, username, email, real_name, nickname, status, password_hash, login_fail_count, last_login_at, login_locked_until, deleted, created_at, updated_at
                        FROM users
                        WHERE id = ?
                        """,
                USER_ROW_MAPPER,
                id
        );
        return users.stream().findFirst();
    }

    public Optional<UserRecord> findActiveById(Long id) {
        List<UserRecord> users = jdbcTemplate.query("""
                        SELECT id, user_code, username, email, real_name, nickname, status, password_hash, login_fail_count, last_login_at, login_locked_until, deleted, created_at, updated_at
                        FROM users
                        WHERE id = ?
                          AND deleted = FALSE
                        """,
                USER_ROW_MAPPER,
                id
        );
        return users.stream().findFirst();
    }

    public Optional<UserRecord> findByUsername(String username) {
        List<UserRecord> users = jdbcTemplate.query("""
                        SELECT id, user_code, username, email, real_name, nickname, status, password_hash, login_fail_count, last_login_at, login_locked_until, deleted, created_at, updated_at
                        FROM users
                        WHERE username = ?
                          AND deleted = FALSE
                        """,
                USER_ROW_MAPPER,
                username
        );
        return users.stream().findFirst();
    }

    public long insert(String username, String email, String passwordHash, String realName, String nickname, String status, Long operatorId) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO users (username, email, password_hash, real_name, nickname, status, created_by, updated_by)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """, new String[]{"id"});
            statement.setString(1, username);
            statement.setString(2, email);
            statement.setString(3, passwordHash);
            statement.setString(4, realName);
            statement.setString(5, nickname);
            statement.setString(6, status);
            statement.setObject(7, operatorId, Types.BIGINT);
            statement.setObject(8, operatorId, Types.BIGINT);
            return statement;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public void updateUserCode(Long userId, String userCode, Long operatorId) {
        jdbcTemplate.update("""
                        UPDATE users
                        SET user_code = ?, updated_by = ?, updated_at = NOW()
                        WHERE id = ?
                        """,
                userCode,
                operatorId,
                userId
        );
    }

    public void updateUser(Long userId, String realName, String nickname, String status, Long operatorId) {
        jdbcTemplate.update("""
                        UPDATE users
                        SET real_name = ?, nickname = ?, status = ?, updated_by = ?, updated_at = NOW()
                        WHERE id = ?
                          AND deleted = FALSE
                        """,
                realName,
                nickname,
                status,
                operatorId,
                userId
        );
    }

    public void updatePassword(Long userId, String passwordHash, Long operatorId) {
        jdbcTemplate.update("""
                        UPDATE users
                        SET password_hash = ?, login_fail_count = 0, login_locked_until = NULL, updated_by = ?, updated_at = NOW()
                        WHERE id = ?
                          AND deleted = FALSE
                        """,
                passwordHash,
                operatorId,
                userId
        );
    }

    public void logicalDelete(Long userId, Long operatorId) {
        jdbcTemplate.update("""
                        UPDATE users
                        SET deleted = TRUE, status = 'DISABLED', updated_by = ?, updated_at = NOW()
                        WHERE id = ?
                          AND deleted = FALSE
                        """,
                operatorId,
                userId
        );
    }

    public void replaceRoles(Long userId, List<Long> roleIds) {
        jdbcTemplate.update("DELETE FROM user_roles WHERE user_id = ?", userId);
        roleIds.forEach(roleId -> jdbcTemplate.update("""
                        INSERT INTO user_roles (user_id, role_id)
                        VALUES (?, ?)
                        """,
                userId,
                roleId
        ));
    }

    public List<UserRoleMapping> findRoleMappingsByUserIds(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return List.of();
        }
        String placeholders = userIds.stream().map(id -> "?").reduce((left, right) -> left + "," + right).orElse("?");
        return jdbcTemplate.query("""
                        SELECT ur.user_id, ur.role_id, r.code AS role_code, r.name AS role_name
                        FROM user_roles ur
                        JOIN roles r ON r.id = ur.role_id
                        WHERE ur.user_id IN (%s)
                          AND r.deleted = FALSE
                        ORDER BY ur.user_id, ur.role_id
                        """.formatted(placeholders),
                (rs, rowNum) -> new UserRoleMapping(
                        rs.getLong("user_id"),
                        rs.getLong("role_id"),
                        rs.getString("role_code"),
                        rs.getString("role_name")
                ),
                userIds.toArray()
        );
    }

    public Map<Long, List<RoleRecord>> findRolesByUserIds(List<Long> userIds) {
        Map<Long, List<RoleRecord>> rolesByUserId = new HashMap<>();
        Map<Long, List<RoleRecord>> mutableMap = new LinkedHashMap<>();
        findRoleMappingsByUserIds(userIds).forEach(mapping -> mutableMap
                .computeIfAbsent(mapping.userId(), key -> new java.util.ArrayList<>())
                .add(new RoleRecord(mapping.roleId(), mapping.roleCode(), mapping.roleName(), null)));
        rolesByUserId.putAll(mutableMap);
        return rolesByUserId;
    }

    public List<UserRecord> searchUsers(String keyword, String status, Long roleId) {
        StringBuilder sql = new StringBuilder("""
                SELECT DISTINCT u.id, u.user_code, u.username, u.email, u.real_name, u.nickname, u.status, u.password_hash, u.login_fail_count, u.last_login_at, u.login_locked_until, u.deleted, u.created_at, u.updated_at
                FROM users u
                LEFT JOIN user_roles ur ON ur.user_id = u.id
                WHERE u.deleted = FALSE
                """);
        Map<String, Object> args = new LinkedHashMap<>();
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (LOWER(u.username) LIKE ? OR LOWER(u.real_name) LIKE ? OR LOWER(COALESCE(u.user_code, '')) LIKE ?)");
            String like = "%" + keyword.trim().toLowerCase() + "%";
            args.put("keyword1", like);
            args.put("keyword2", like);
            args.put("keyword3", like);
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND u.status = ?");
            args.put("status", status.trim().toUpperCase());
        }
        if (roleId != null) {
            sql.append(" AND ur.role_id = ?");
            args.put("roleId", roleId);
        }
        sql.append(" ORDER BY u.created_at DESC, u.id DESC");
        return jdbcTemplate.query(sql.toString(), USER_ROW_MAPPER, args.values().toArray());
    }

    public boolean isLoginLocked(Long userId, java.time.OffsetDateTime now) {
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM users
                        WHERE id = ?
                          AND login_locked_until IS NOT NULL
                          AND login_locked_until > ?
                        """,
                Integer.class,
                userId,
                now
        );
        return count != null && count > 0;
    }

    public int increaseLoginFailCount(Long userId,
                                      java.time.OffsetDateTime now,
                                      int maxAttempts,
                                      int lockMinutes) {
        UserRecord user = findActiveById(userId).orElseThrow();
        int failCount = (user.loginFailCount() == null ? 0 : user.loginFailCount()) + 1;
        java.time.OffsetDateTime lockedUntil = failCount >= maxAttempts ? now.plusMinutes(lockMinutes) : null;
        jdbcTemplate.update("""
                        UPDATE users
                        SET login_fail_count = ?, login_locked_until = ?, updated_at = NOW()
                        WHERE id = ?
                        """,
                failCount,
                lockedUntil,
                userId
        );
        return failCount;
    }

    public void resetLoginFailCount(Long userId, java.time.OffsetDateTime now) {
        jdbcTemplate.update("""
                        UPDATE users
                        SET login_fail_count = 0,
                            login_locked_until = NULL,
                            last_login_at = ?,
                            updated_at = NOW()
                        WHERE id = ?
                        """,
                now,
                userId
        );
    }
}
