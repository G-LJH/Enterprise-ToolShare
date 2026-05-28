package com.toolshare.user.web;

import com.toolshare.user.model.RoleRecord;
import com.toolshare.user.model.UserRecord;

import java.time.OffsetDateTime;
import java.util.List;

public record UserListItemResponse(
        Long id,
        String userCode,
        String username,
        String realName,
        String status,
        OffsetDateTime createdAt,
        List<RoleResponse> roles
) {
    public static UserListItemResponse from(UserRecord user, List<RoleRecord> roles) {
        return new UserListItemResponse(
                user.id(),
                user.userCode(),
                user.username(),
                user.realName(),
                user.status(),
                user.createdAt(),
                roles.stream().map(RoleResponse::from).toList()
        );
    }
}
