package com.toolshare.user.web;

import com.toolshare.user.model.RoleRecord;
import com.toolshare.user.model.UserRecord;

import java.time.OffsetDateTime;
import java.util.List;

public record UserDetailResponse(
        Long id,
        String userCode,
        String username,
        String realName,
        String nickname,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<RoleResponse> roles
) {
    public static UserDetailResponse from(UserRecord user, List<RoleRecord> roles) {
        return new UserDetailResponse(
                user.id(),
                user.userCode(),
                user.username(),
                user.realName(),
                user.nickname(),
                user.status(),
                user.createdAt(),
                user.updatedAt(),
                roles.stream().map(RoleResponse::from).toList()
        );
    }
}
