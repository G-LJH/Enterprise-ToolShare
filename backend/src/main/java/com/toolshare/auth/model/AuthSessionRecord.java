package com.toolshare.auth.model;

import java.time.OffsetDateTime;

public record AuthSessionRecord(
        Long id,
        Long userId,
        String tokenHash,
        OffsetDateTime expiresAt,
        OffsetDateTime lastAccessedAt,
        OffsetDateTime revokedAt,
        OffsetDateTime createdAt
) {
}
