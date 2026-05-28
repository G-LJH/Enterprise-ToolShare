package com.toolshare.user.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toolshare.config.RequestUserInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserAdminControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void adminShouldManageUsersAndWriteAuditLogs() throws Exception {
        Long adminId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = 'admin'", Long.class);
        Long userRoleId = jdbcTemplate.queryForObject("SELECT id FROM roles WHERE code = 'USER'", Long.class);

        CreateUserRequest createRequest = new CreateUserRequest(
                "casey",
                "Casey Zhang",
                "Casey",
                "Casey@123456",
                "ACTIVE",
                List.of(userRoleId)
        );

        mockMvc.perform(post("/api/admin/users")
                        .header(RequestUserInterceptor.USER_ID_HEADER, adminId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("casey"))
                .andExpect(jsonPath("$.data.roles[0].code").value("USER"));

        Long createdUserId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = 'casey'", Long.class);
        String createdUserCode = jdbcTemplate.queryForObject("SELECT user_code FROM users WHERE id = ?", String.class, createdUserId);
        assertThat(createdUserCode).isEqualTo("USR%06d".formatted(createdUserId));

        UpdateUserRequest updateRequest = new UpdateUserRequest(
                "Casey Updated",
                "Casey U",
                "DISABLED",
                List.of(userRoleId)
        );

        mockMvc.perform(put("/api/admin/users/{userId}", createdUserId)
                        .header(RequestUserInterceptor.USER_ID_HEADER, adminId)
                        .contentType("application/json")
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DISABLED"));

        ResetPasswordRequest resetRequest = new ResetPasswordRequest("NewSecure@123456");

        mockMvc.perform(post("/api/admin/users/{userId}/reset-password", createdUserId)
                        .header(RequestUserInterceptor.USER_ID_HEADER, adminId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(resetRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("success"));

        mockMvc.perform(delete("/api/admin/users/{userId}", createdUserId)
                        .header(RequestUserInterceptor.USER_ID_HEADER, adminId))
                .andExpect(status().isOk());

        Integer deletedFlag = jdbcTemplate.queryForObject("SELECT CASE WHEN deleted THEN 1 ELSE 0 END FROM users WHERE id = ?", Integer.class, createdUserId);
        assertThat(deletedFlag).isEqualTo(1);

        Integer auditCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM audit_logs
                WHERE object_type = 'USER'
                  AND object_id = ?
                """, Integer.class, String.valueOf(createdUserId));
        assertThat(auditCount).isEqualTo(4);
    }

    @Test
    void nonAdminShouldBeRejectedFromUserManagementApis() throws Exception {
        Long userRoleId = jdbcTemplate.queryForObject("SELECT id FROM roles WHERE code = 'USER'", Long.class);
        jdbcTemplate.update("""
                INSERT INTO users (username, email, password_hash, real_name, nickname, status, created_by, updated_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, "member", "member@example.com", "hashed", "Member User", "Member", "ACTIVE", 0L, 0L);
        Long memberId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = 'member'", Long.class);
        jdbcTemplate.update("UPDATE users SET user_code = ? WHERE id = ?", "USR%06d".formatted(memberId), memberId);
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", memberId, userRoleId);

        mockMvc.perform(get("/api/admin/users")
                        .header(RequestUserInterceptor.USER_ID_HEADER, memberId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("当前账号无权访问该管理资源"));
    }

    @Test
    void disabledUserShouldBeRejectedEvenIfRoleExists() throws Exception {
        Long adminRoleId = jdbcTemplate.queryForObject("SELECT id FROM roles WHERE code = 'ADMIN'", Long.class);
        jdbcTemplate.update("""
                INSERT INTO users (username, email, password_hash, real_name, nickname, status, created_by, updated_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, "disabled_admin", "disabled-admin@example.com", "hashed", "Disabled Admin", "DA", "DISABLED", 0L, 0L);
        Long disabledAdminId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = 'disabled_admin'", Long.class);
        jdbcTemplate.update("UPDATE users SET user_code = ? WHERE id = ?", "USR%06d".formatted(disabledAdminId), disabledAdminId);
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", disabledAdminId, adminRoleId);

        mockMvc.perform(get("/api/admin/users")
                        .header(RequestUserInterceptor.USER_ID_HEADER, disabledAdminId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("账号已停用，无法访问当前资源"));
    }

    @Test
    void rolesEndpointShouldExposeSeededRoles() throws Exception {
        Long adminId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = 'admin'", Long.class);

        String response = mockMvc.perform(get("/api/admin/roles")
                        .header(RequestUserInterceptor.USER_ID_HEADER, adminId))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Map<?, ?> payload = objectMapper.readValue(response, Map.class);
        assertThat(payload.get("data")).isNotNull();
    }

    @Test
    void shouldRejectRemovingAdminRoleFromAdminAccount() throws Exception {
        Long adminId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = 'admin'", Long.class);
        Long userRoleId = jdbcTemplate.queryForObject("SELECT id FROM roles WHERE code = 'USER'", Long.class);

        UpdateUserRequest request = new UpdateUserRequest(
                "System Administrator",
                "Admin",
                "ACTIVE",
                List.of(userRoleId)
        );

        mockMvc.perform(put("/api/admin/users/{userId}", adminId)
                        .header(RequestUserInterceptor.USER_ID_HEADER, adminId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("管理员权限固定，不能移除 ADMIN 角色"));
    }

    @Test
    void shouldRejectDeletingAdminAccount() throws Exception {
        Long adminId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = 'admin'", Long.class);

        jdbcTemplate.update("""
                INSERT INTO users (username, email, password_hash, real_name, nickname, status, created_by, updated_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, "ops_admin", "ops-admin@example.com", "hashed", "Ops Admin", "OA", "ACTIVE", adminId, adminId);
        Long adminRoleId = jdbcTemplate.queryForObject("SELECT id FROM roles WHERE code = 'ADMIN'", Long.class);
        Long opsAdminId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = 'ops_admin'", Long.class);
        jdbcTemplate.update("UPDATE users SET user_code = ? WHERE id = ?", "USR%06d".formatted(opsAdminId), opsAdminId);
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", opsAdminId, adminRoleId);

        mockMvc.perform(delete("/api/admin/users/{userId}", opsAdminId)
                        .header(RequestUserInterceptor.USER_ID_HEADER, adminId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("管理员账号不能删除"));
    }
}
