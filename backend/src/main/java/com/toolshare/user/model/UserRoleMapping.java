package com.toolshare.user.model;

public record UserRoleMapping(
        Long userId,
        Long roleId,
        String roleCode,
        String roleName
) {
}
