package com.toolshare.user.web;

import com.toolshare.user.model.RoleRecord;

public record RoleResponse(
        Long id,
        String code,
        String name
) {
    public static RoleResponse from(RoleRecord role) {
        return new RoleResponse(role.id(), role.code(), role.name());
    }
}
