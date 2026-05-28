package com.toolshare.auth.web;

import com.toolshare.security.CurrentUser;

import java.util.List;

public record CurrentUserResponse(
        Long id,
        String userCode,
        String username,
        String realName,
        String status,
        List<String> roleCodes
) {
    public static CurrentUserResponse from(CurrentUser currentUser) {
        return new CurrentUserResponse(
                currentUser.id(),
                currentUser.userCode(),
                currentUser.username(),
                currentUser.realName(),
                currentUser.status(),
                currentUser.roleCodes().stream().sorted().toList()
        );
    }
}
