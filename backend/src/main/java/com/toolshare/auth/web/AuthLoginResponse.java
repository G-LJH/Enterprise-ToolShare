package com.toolshare.auth.web;

import java.time.OffsetDateTime;

public record AuthLoginResponse(
        String accessToken,
        OffsetDateTime expiresAt,
        CurrentUserResponse user
) {
}
