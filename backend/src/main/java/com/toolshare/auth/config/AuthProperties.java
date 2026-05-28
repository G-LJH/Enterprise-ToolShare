package com.toolshare.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "space.auth")
public record AuthProperties(
        int sessionTtlHours,
        int loginFailMaxAttempts,
        int loginLockMinutes
) {
}
