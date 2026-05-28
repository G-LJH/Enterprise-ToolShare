package com.toolshare.auth.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SessionTokenServiceTests {

    private final SessionTokenService sessionTokenService = new SessionTokenService();

    @Test
    void shouldGenerateUniqueOpaqueTokenAndStableHash() {
        String tokenA = sessionTokenService.generateToken();
        String tokenB = sessionTokenService.generateToken();

        assertThat(tokenA).isNotBlank();
        assertThat(tokenB).isNotBlank();
        assertThat(tokenA).isNotEqualTo(tokenB);
        assertThat(sessionTokenService.hashToken(tokenA)).isEqualTo(sessionTokenService.hashToken(tokenA));
        assertThat(sessionTokenService.hashToken(tokenA)).isNotEqualTo(sessionTokenService.hashToken(tokenB));
    }
}
