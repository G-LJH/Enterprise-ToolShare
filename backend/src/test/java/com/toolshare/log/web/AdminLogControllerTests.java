package com.toolshare.log.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toolshare.auth.service.PasswordService;
import com.toolshare.config.RequestUserInterceptor;
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
class AdminLogControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordService passwordService;

    @Test
    void adminShouldListAuditAndOperationLogs() throws Exception {
        Long adminId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = 'admin'", Long.class);
        Long userId = seedUser("log_casey", "log-casey@example.com", "Log Casey");
        jdbcTemplate.update("""
                INSERT INTO tags (name, description, created_by, updated_by)
                VALUES (?, ?, ?, ?)
                """, "日志标签", "日志测试标签", adminId, adminId);
        Long tagId = jdbcTemplate.queryForObject("SELECT id FROM tags WHERE name = '日志标签'", Long.class);

        mockMvc.perform(post("/api/tools/submissions")
                        .header(RequestUserInterceptor.USER_ID_HEADER, userId)
                        .contentType("application/json")
                        .content("""
                                {
                                  "name":"日志测试工具",
                                  "summary":"用于验证操作日志",
                                  "description":"验证只记录提交修改删除操作",
                                  "url":"https://log-tool.example.com",
                                  "usageGuide":"提交后查看日志",
                                  "tagIds":[%d]
                                }
                                """.formatted(tagId)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/logs/audit")
                        .header(RequestUserInterceptor.USER_ID_HEADER, adminId)
                        .param("action", "TOOL_SUBMITTED")
                        .param("objectType", "TOOL")
                        .param("keyword", "日志测试工具"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].action").value("TOOL_SUBMITTED"))
                .andExpect(jsonPath("$.data.items[0].detail").value(org.hamcrest.Matchers.containsString("日志测试工具")));

        mockMvc.perform(get("/api/admin/logs/operations")
                        .header(RequestUserInterceptor.USER_ID_HEADER, adminId)
                        .param("module", "TOOL")
                        .param("success", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").isNumber())
                .andExpect(jsonPath("$.data.items[0].module").value("TOOL"))
                .andExpect(jsonPath("$.data.items[0].operation").value("TOOL_SUBMIT"));

        Integer operationCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM operation_logs
                WHERE module = 'TOOL'
                  AND operation = 'TOOL_SUBMIT'
                  AND success = TRUE
                """, Integer.class);
        assertThat(operationCount).isEqualTo(1);
    }

    @Test
    void operationLogsShouldKeepOnlyLatestTwoHundredRows() throws Exception {
        Long adminId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = 'admin'", Long.class);
        for (int i = 0; i < 205; i++) {
            jdbcTemplate.update("""
                    INSERT INTO operation_logs (operation, module, user_id, success, message)
                    VALUES (?, 'TOOL', ?, TRUE, ?)
                    """, "TOOL_UPDATE", adminId, "seed-" + i);
        }

        mockMvc.perform(post("/api/admin/workflows")
                        .header(RequestUserInterceptor.USER_ID_HEADER, adminId)
                        .contentType("application/json")
                        .content("""
                                {
                                  "name":"日志保留工作流",
                                  "scenario":"日志测试",
                                  "description":"用于触发日志裁剪",
                                  "steps":"创建后检查操作日志数量。",
                                  "featured":false,
                                  "toolIds":[]
                                }
                                """))
                .andExpect(status().isOk());

        Integer operationCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM operation_logs", Integer.class);
        assertThat(operationCount).isEqualTo(200);

        Integer oldSeedCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM operation_logs
                WHERE message = 'seed-0'
                """, Integer.class);
        assertThat(oldSeedCount).isZero();
    }

    @Test
    void nonAdminShouldBeRejectedFromLogApis() throws Exception {
        Long userRoleId = jdbcTemplate.queryForObject("SELECT id FROM roles WHERE code = 'USER'", Long.class);
        jdbcTemplate.update("""
                INSERT INTO users (username, email, password_hash, real_name, nickname, status, created_by, updated_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                "member-log",
                "member-log@example.com",
                passwordService.encode("Member@123456"),
                "Member Log",
                "ML",
                "ACTIVE",
                0L,
                0L
        );
        Long memberId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = 'member-log'", Long.class);
        jdbcTemplate.update("UPDATE users SET user_code = ? WHERE id = ?", "USR%06d".formatted(memberId), memberId);
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", memberId, userRoleId);

        mockMvc.perform(get("/api/admin/logs/audit")
                        .header(RequestUserInterceptor.USER_ID_HEADER, memberId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("当前账号无权访问该管理资源"));
    }

    private Long seedUser(String username, String email, String realName) {
        Long userRoleId = jdbcTemplate.queryForObject("SELECT id FROM roles WHERE code = 'USER'", Long.class);
        jdbcTemplate.update("""
                INSERT INTO users (username, email, password_hash, real_name, nickname, status, user_code, created_by, updated_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                username,
                email,
                passwordService.encode("User@123456"),
                realName,
                realName,
                "ACTIVE",
                "USR" + Math.abs(username.hashCode()),
                0L,
                0L
        );
        Long userId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = ?", Long.class, username);
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, userRoleId);
        return userId;
    }
}
