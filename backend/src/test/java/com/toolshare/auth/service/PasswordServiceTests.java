package com.toolshare.auth.service;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordServiceTests {

    private final PasswordService passwordService = new PasswordService(new BCryptPasswordEncoder());

    @Test
    void shouldValidateEncodedPassword() {
        String passwordHash = passwordService.encode("Admin@123456");

        assertThat(passwordService.matches("Admin@123456", passwordHash)).isTrue();
        assertThat(passwordService.matches("wrong-password", passwordHash)).isFalse();
    }
}
