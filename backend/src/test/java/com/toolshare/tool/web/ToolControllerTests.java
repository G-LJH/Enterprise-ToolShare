package com.toolshare.tool.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toolshare.auth.service.PasswordService;
import com.toolshare.config.RequestUserInterceptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ToolControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordService passwordService;

    @BeforeEach
    void resetToolReviewConfig() {
        jdbcTemplate.update("""
                UPDATE system_configs
                SET config_value = 'true'
                WHERE config_key = 'TOOL_REVIEW_ENABLED'
                """);
    }

    @Test
    void adminShouldManageToolsAndWriteAuditLogs() throws Exception {
        Long adminId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = 'admin'", Long.class);
        Long aiTagId = seedTag(adminId, "AI 助手");
        ToolUpsertRequest request = new ToolUpsertRequest(
                "Notion AI",
                "知识库与文档协作",
                "支持文档沉淀、知识协作与 AI 辅助整理。",
                "https://www.notion.so/product/ai",
                "创建知识库空间，整理常用提示词，再把使用说明沉淀到团队主页。",
                java.util.List.of(aiTagId)
        );

        String createResponse = mockMvc.perform(post("/api/admin/tools")
                        .header(RequestUserInterceptor.USER_ID_HEADER, adminId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Notion AI"))
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.tags[0].name").value("AI 助手"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long toolId = objectMapper.readTree(createResponse).path("data").path("id").asLong();

        mockMvc.perform(put("/api/admin/tools/{toolId}", toolId)
                        .header(RequestUserInterceptor.USER_ID_HEADER, adminId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new ToolUpsertRequest(
                                "Notion AI Updated",
                                "知识库与文档协作平台",
                                "沉淀团队 SOP、FAQ 与 AI 助手入口。",
                                "https://www.notion.so/product/ai-updated",
                                "先建立模板，再按部门复制使用。",
                                java.util.List.of(aiTagId)
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Notion AI Updated"));

        mockMvc.perform(post("/api/admin/tools/{toolId}/offline", toolId)
                        .header(RequestUserInterceptor.USER_ID_HEADER, adminId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("success"));

        String statusValue = jdbcTemplate.queryForObject("SELECT status FROM tools WHERE id = ?", String.class, toolId);
        assertThat(statusValue).isEqualTo("OFFLINE");

        Integer auditCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM audit_logs
                WHERE object_type = 'TOOL'
                  AND object_id = ?
                """, Integer.class, String.valueOf(toolId));
        assertThat(auditCount).isEqualTo(3);
    }

    @Test
    void adminShouldCreateToolWithFreshlyCreatedTag() throws Exception {
        Long adminId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = 'admin'", Long.class);

        String tagResponse = mockMvc.perform(post("/api/tags")
                        .header(RequestUserInterceptor.USER_ID_HEADER, adminId)
                        .contentType("application/json")
                        .content("""
                                {"name":"即时新建标签","description":""}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("即时新建标签"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long tagId = objectMapper.readTree(tagResponse).path("data").path("id").asLong();

        mockMvc.perform(post("/api/admin/tools")
                        .header(RequestUserInterceptor.USER_ID_HEADER, adminId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new ToolUpsertRequest(
                                "Fresh Tag Tool",
                                "刚创建标签后直接绑定",
                                "覆盖新增工具时现场创建标签的场景。",
                                "https://example.com/fresh-tag-tool",
                                "先建标签，再立即创建工具。",
                                java.util.List.of(tagId)
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Fresh Tag Tool"))
                .andExpect(jsonPath("$.data.tags[0].id").value(tagId))
                .andExpect(jsonPath("$.data.tags[0].name").value("即时新建标签"));
    }

    @Test
    void userShouldSubmitToolAndSeeItInMyList() throws Exception {
        Long userId = seedUser("submitter", "submitter@example.com", "Submitter User");
        Long designTagId = seedTag(0L, "设计协作");

        String response = mockMvc.perform(post("/api/tools/submissions")
                        .header(RequestUserInterceptor.USER_ID_HEADER, userId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new ToolUpsertRequest(
                                "Figma",
                                "界面协作设计工具",
                                "支持设计、评审与原型协作。",
                                "https://www.figma.com",
                                "使用团队组件库创建原型，并把链接同步到项目文档。",
                                java.util.List.of(designTagId)
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$.data.submission.status").value("PENDING"))
                .andExpect(jsonPath("$.data.tags[0].name").value("设计协作"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long toolId = objectMapper.readTree(response).path("data").path("id").asLong();

        mockMvc.perform(get("/api/tools")
                        .header(RequestUserInterceptor.USER_ID_HEADER, userId)
                        .param("mine", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(toolId))
                .andExpect(jsonPath("$.data[0].status").value("PENDING_REVIEW"));

        Integer submissionCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tool_submissions WHERE tool_id = ?", Integer.class, toolId);
        assertThat(submissionCount).isEqualTo(1);
    }

    @Test
    void submitToolShouldAutoApproveWhenReviewConfigDisabled() throws Exception {
        Long userId = seedUser("auto-approved", "auto-approved@example.com", "Auto Approved User");
        Long tagId = seedTag(0L, "自动发布");
        jdbcTemplate.update("""
                UPDATE system_configs
                SET config_value = 'false'
                WHERE config_key = 'TOOL_REVIEW_ENABLED'
                """);

        mockMvc.perform(post("/api/tools/submissions")
                        .header(RequestUserInterceptor.USER_ID_HEADER, userId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new ToolUpsertRequest(
                                "Lark Base",
                                "轻量数据管理工具",
                                "适合简单台账和流程协作。",
                                "https://www.larksuite.com",
                                "按业务场景建立字段并共享给团队成员。",
                                java.util.List.of(tagId)
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.submission.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.submission.remark").value("审核开关关闭，系统自动通过"));
    }

    @Test
    void invalidUrlShouldBeRejected() throws Exception {
        Long userId = seedUser("url-checker", "url-checker@example.com", "URL Checker");

        mockMvc.perform(post("/api/tools/submissions")
                        .header(RequestUserInterceptor.USER_ID_HEADER, userId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new ToolUpsertRequest(
                                "Broken Tool",
                                "bad url",
                                "链接不合法时应当拦截",
                                "javascript:alert(1)",
                                "随便写点说明",
                                java.util.List.of()
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("工具链接必须为合法的 http 或 https 地址"));
    }

    @Test
    void toolListShouldSupportTagFilterAndSort() throws Exception {
        Long adminId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = 'admin'", Long.class);
        Long aiTagId = seedTag(adminId, "AI");
        Long designTagId = seedTag(adminId, "Design");

        Long alphaToolId = createTool(adminId, "Alpha Tool", "https://alpha.example.com", java.util.List.of(aiTagId), 5);
        Long betaToolId = createTool(adminId, "Beta Tool", "https://beta.example.com", java.util.List.of(designTagId), 15);

        mockMvc.perform(get("/api/tools")
                        .header(RequestUserInterceptor.USER_ID_HEADER, adminId)
                        .param("tagIds", String.valueOf(designTagId))
                        .param("sortBy", "starCount")
                        .param("sortOrder", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(betaToolId))
                .andExpect(jsonPath("$.data[0].tags[0].name").value("Design"));

        mockMvc.perform(get("/api/tools")
                        .header(RequestUserInterceptor.USER_ID_HEADER, adminId)
                        .param("keyword", "alpha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(alphaToolId));
    }

    @Test
    void nonAdminShouldNotAccessAdminToolApis() throws Exception {
        Long userId = seedUser("member-tool", "member-tool@example.com", "Member Tool");

        mockMvc.perform(get("/api/admin/tools")
                        .header(RequestUserInterceptor.USER_ID_HEADER, userId))
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

    private Long seedTag(Long operatorId, String name) {
        jdbcTemplate.update("""
                INSERT INTO tags (name, description, created_by, updated_by)
                VALUES (?, ?, ?, ?)
                """, name, name + " 标签", operatorId, operatorId);
        return jdbcTemplate.queryForObject("SELECT id FROM tags WHERE name = ?", Long.class, name);
    }

    private Long createTool(Long operatorId, String name, String url, java.util.List<Long> tagIds, int starCount) {
        jdbcTemplate.update("""
                INSERT INTO tools (name, summary, description, url, usage_guide, recommender_id, status, star_count, favorite_count, comment_count, created_by, updated_by)
                VALUES (?, ?, ?, ?, ?, ?, 'APPROVED', ?, 0, 0, ?, ?)
                """,
                name,
                name + " summary",
                name + " description",
                url,
                name + " usage",
                operatorId,
                starCount,
                operatorId,
                operatorId
        );
        Long toolId = jdbcTemplate.queryForObject("SELECT id FROM tools WHERE name = ?", Long.class, name);
        tagIds.forEach(tagId -> jdbcTemplate.update("INSERT INTO tool_tags (tool_id, tag_id) VALUES (?, ?)", toolId, tagId));
        return toolId;
    }
}
