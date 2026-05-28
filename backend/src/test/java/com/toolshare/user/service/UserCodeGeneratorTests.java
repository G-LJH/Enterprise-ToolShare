package com.toolshare.user.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserCodeGeneratorTests {

    private final UserCodeGenerator userCodeGenerator = new UserCodeGenerator();

    @Test
    void shouldGenerateStableUserCodeFormat() {
        assertThat(userCodeGenerator.generate(1L)).isEqualTo("USR000001");
        assertThat(userCodeGenerator.generate(12345L)).isEqualTo("USR012345");
    }
}
