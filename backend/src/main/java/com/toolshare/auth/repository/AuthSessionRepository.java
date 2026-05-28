package com.toolshare.auth.repository;

import com.toolshare.auth.model.AuthSessionRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class AuthSessionRepository {

    private static final RowMapper<AuthSessionRecord> AUTH_SESSION_ROW_MAPPER = (rs, rowNum) -> new AuthSessionRecord(
            rs.getLong("id"),
            rs.getLong("user_id"),
            rs.getString("token_hash"),
            rs.getObject("expires_at", OffsetDateTime.class),
            rs.getObject("last_accessed_at", OffsetDateTime.class),
            rs.getObject("revoked_at", OffsetDateTime.class),
            rs.getObject("created_at", OffsetDateTime.class)
    );

    private final JdbcTemplate jdbcTemplate;

    public AuthSessionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long insert(Long userId, String tokenHash, OffsetDateTime expiresAt) {
        jdbcTemplate.update("""
                        INSERT INTO auth_sessions (user_id, token_hash, expires_at)
                        VALUES (?, ?, ?)
                        """,
                userId,
                tokenHash,
                expiresAt
        );
        return jdbcTemplate.queryForObject("SELECT id FROM auth_sessions WHERE token_hash = ?", Long.class, tokenHash);
    }

    public Optional<AuthSessionRecord> findActiveByTokenHash(String tokenHash, OffsetDateTime now) {
        List<AuthSessionRecord> sessions = jdbcTemplate.query("""
                        SELECT id, user_id, token_hash, expires_at, last_accessed_at, revoked_at, created_at
                        FROM auth_sessions
                        WHERE token_hash = ?
                          AND revoked_at IS NULL
                          AND expires_at > ?
                        """,
                AUTH_SESSION_ROW_MAPPER,
                tokenHash,
                now
        );
        return sessions.stream().findFirst();
    }

    public void touch(Long sessionId) {
        jdbcTemplate.update("""
                        UPDATE auth_sessions
                        SET last_accessed_at = NOW()
                        WHERE id = ?
                        """,
                sessionId
        );
    }

    public void revokeByTokenHash(String tokenHash) {
        jdbcTemplate.update("""
                        UPDATE auth_sessions
                        SET revoked_at = NOW()
                        WHERE token_hash = ?
                          AND revoked_at IS NULL
                        """,
                tokenHash
        );
    }
}
