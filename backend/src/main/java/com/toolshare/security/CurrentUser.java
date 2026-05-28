package com.toolshare.security;

import java.util.Set;

public record CurrentUser(
        Long id,
        String userCode,
        String username,
        String realName,
        String status,
        Set<String> roleCodes
) {

    public boolean hasAnyRole(Set<String> requiredRoles) {
        return roleCodes.stream().anyMatch(requiredRoles::contains);
    }
}
