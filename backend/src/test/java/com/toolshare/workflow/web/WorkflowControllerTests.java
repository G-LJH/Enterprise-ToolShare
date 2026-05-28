package com.toolshare.workflow.web;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WorkflowControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordService passwordService;

    @Test
    void adminShouldManageWorkflowAndUserShouldBrowseIt() throws Exception {
        Long adminId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = 'admin'", Long.class);
        Long toolId = createTool(adminId, "Workflow Tool A", "https://workflow-a.example.com");
        Long toolId2 = createTool(adminId, "Workflow Tool B", "https://workflow-b.example.com");

        String response = mockMvc.perform(post("/api/admin/workflows")
                        .header(RequestUserInterceptor.USER_ID_HEADER, adminId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new WorkflowUpsertRequest(
                                "调研报告工作流",
                                "行业调研",
                                "沉淀常见调研报告产出方式。",
                                "先搜集资料，再总结结构，最后输出报告。",
                                true,
                                java.util.List.of(toolId, toolId2)
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("调研报告工作流"))
                .andExpect(jsonPath("$.data.featured").value(true))
                .andExpect(jsonPath("$.data.tools.length()").value(2))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long workflowId = objectMapper.readTree(response).path("data").path("id").asLong();

        mockMvc.perform(get("/api/workflows")
                        .param("featuredOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(workflowId))
                .andExpect(jsonPath("$.data[0].toolCount").value(2));

        mockMvc.perform(get("/api/workflows/{workflowId}", workflowId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scenario").value("行业调研"))
                .andExpect(jsonPath("$.data.tools[0].id").value(toolId));

        mockMvc.perform(put("/api/admin/workflows/{workflowId}", workflowId)
                        .header(RequestUserInterceptor.USER_ID_HEADER, adminId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new WorkflowUpsertRequest(
                                "调研报告工作流 v2",
                                "行业调研",
                                "更新为更稳定的调研流程。",
                                "先定义问题，再搜集资料，最后统一输出。",
                                false,
                                java.util.List.of(toolId2, toolId)
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("调研报告工作流 v2"))
                .andExpect(jsonPath("$.data.featured").value(false))
                .andExpect(jsonPath("$.data.tools[0].id").value(toolId2));

        mockMvc.perform(delete("/api/admin/workflows/{workflowId}", workflowId)
                        .header(RequestUserInterceptor.USER_ID_HEADER, adminId))
                .andExpect(status().isOk());

        Boolean deleted = jdbcTemplate.queryForObject("SELECT deleted FROM workflows WHERE id = ?", Boolean.class, workflowId);
        assertThat(deleted).isTrue();
    }

    @Test
    void nonAdminShouldNotManageWorkflow() throws Exception {
        Long userId = seedUser("workflow-user", "workflow-user@example.com", "Workflow User");
        Long toolId = createTool(userId, "Workflow Tool User", "https://workflow-user-tool.example.com");

        mockMvc.perform(post("/api/admin/workflows")
                        .header(RequestUserInterceptor.USER_ID_HEADER, userId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new WorkflowUpsertRequest(
                                "个人工作流",
                                "个人整理",
                                "个人使用",
                                "步骤说明",
                                false,
                                java.util.List.of(toolId)
                        ))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("当前账号无权访问该管理资源"));
    }

    @Test
    void userShouldSubmitWorkflowFromSubmissionApi() throws Exception {
        Long userId = seedUser("workflow-submit-user", "workflow-submit-user@example.com", "Workflow Submit User");
        Long toolId = createTool(userId, "Workflow Submit Tool", "https://workflow-submit-tool.example.com");

        String response = mockMvc.perform(post("/api/workflows/submissions")
                        .header(RequestUserInterceptor.USER_ID_HEADER, userId)
                        .contentType("application/json")
                        .content("""
                                {
                                  "name":"内容策划流",
                                  "scenario":"内容策划",
                                  "description":"把内容准备步骤整理成固定做法。",
                                  "steps":"先确定主题，再准备素材，最后产出大纲。",
                                  "toolIds":[%d]
                                }
                                """.formatted(toolId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("内容策划流"))
                .andExpect(jsonPath("$.data.featured").value(false))
                .andExpect(jsonPath("$.data.tools[0].id").value(toolId))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long workflowId = objectMapper.readTree(response).path("data").path("id").asLong();

        mockMvc.perform(get("/api/workflows/{workflowId}", workflowId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("内容策划流"))
                .andExpect(jsonPath("$.data.featured").value(false));
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

    private Long createTool(Long operatorId, String name, String url) {
        jdbcTemplate.update("""
                INSERT INTO tools (name, summary, description, url, usage_guide, recommender_id, status, star_count, favorite_count, comment_count, created_by, updated_by)
                VALUES (?, ?, ?, ?, ?, ?, 'APPROVED', 0, 0, 0, ?, ?)
                """,
                name,
                name + " summary",
                name + " description",
                url,
                name + " usage",
                operatorId,
                operatorId,
                operatorId
        );
        return jdbcTemplate.queryForObject("SELECT id FROM tools WHERE name = ?", Long.class, name);
    }
}
