package com.toolshare.user.model;

import java.time.OffsetDateTime;

public record UserRecord(
        Long id,
        String userCode,
        String username,
        String email,
        String realName,
        String nickname,
        String status,
        String passwordHash,
        Integer loginFailCount,
        OffsetDateTime lastLoginAt,
        OffsetDateTime loginLockedUntil,
        boolean deleted,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
