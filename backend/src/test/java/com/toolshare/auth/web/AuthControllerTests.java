package com.toolshare.auth.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toolshare.auth.service.PasswordService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordService passwordService;

    @Test
    void loginShouldReturnTokenAndAllowAuthenticatedRequests() throws Exception {
        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new AuthLoginRequest("admin", "Admin@123456"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.user.username").value("admin"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = objectMapper.readTree(loginResponse).path("data").path("accessToken").asText();
        assertThat(token).isNotBlank();

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("admin"))
                .andExpect(jsonPath("$.data.roleCodes[0]").value("ADMIN"));

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("登录态已失效，请重新登录"));
    }

    @Test
    void loginShouldRejectWrongPasswordAndLockAfterRepeatedFailures() throws Exception {
        jdbcTemplate.update("""
                INSERT INTO users (username, email, password_hash, real_name, nickname, status, user_code, created_by, updated_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                "failing-user",
                "failing-user@example.com",
                passwordService.encode("Failing@123456"),
                "Failing User",
                "Failing",
                "ACTIVE",
                "USR999992",
                0L,
                0L
        );

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(new AuthLoginRequest("failing-user", "wrong-password"))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("用户名或密码错误"));
        }

        Integer failCount = jdbcTemplate.queryForObject("SELECT login_fail_count FROM users WHERE username = 'failing-user'", Integer.class);
        assertThat(failCount).isEqualTo(5);

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new AuthLoginRequest("failing-user", "Failing@123456"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("连续登录失败次数过多，请稍后再试"));
    }

    @Test
    void disabledUserShouldNotBeAbleToLogin() throws Exception {
        jdbcTemplate.update("""
                INSERT INTO users (username, email, password_hash, real_name, nickname, status, user_code, created_by, updated_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                "locked-user",
                "locked-user@example.com",
                passwordService.encode("Locked@123456"),
                "Locked User",
                "Locked",
                "DISABLED",
                "USR999991",
                0L,
                0L
        );

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new AuthLoginRequest("locked-user", "Locked@123456"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("账号已停用，无法登录"));
    }

    @Test
    void meShouldRequireAuthenticatedSession() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("当前请求未登录"));
    }
}
