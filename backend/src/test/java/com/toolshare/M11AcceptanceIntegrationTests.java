package com.toolshare;

import com.fasterxml.jackson.databind.JsonNode;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class M11AcceptanceIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordService passwordService;

    @Test
    void shouldPassCoreCrossModuleAcceptanceFlow() throws Exception {
        seedUser("accept-user", "accept-user@example.com", "Acceptance User", "Accept@123456", false);

        String adminToken = login("admin", "Admin@123456");
        String userToken = login("accept-user", "Accept@123456");

        mockMvc.perform(put("/api/admin/reviews/config/tool-review")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content("""
                                {"toolReviewEnabled":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.toolReviewEnabled").value(true));

        String tagResponse = mockMvc.perform(post("/api/admin/tags")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content("""
                                {"name":"M11 集成标签","description":"M11 验收使用标签"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("M11 集成标签"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long tagId = objectMapper.readTree(tagResponse).path("data").path("id").asLong();

        String submitResponse = mockMvc.perform(post("/api/tools/submissions")
                        .header("Authorization", bearer(userToken))
                        .contentType("application/json")
                        .content("""
                                {
                                  "name":"M11 Acceptance Tool",
                                  "summary":"M11 summary",
                                  "description":"M11 description",
                                  "url":"https://m11-acceptance.example.com",
                                  "usageGuide":"先登录，再提交，再审核，再互动。",
                                  "tagIds":[%d]
                                }
                                """.formatted(tagId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_REVIEW"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long toolId = objectMapper.readTree(submitResponse).path("data").path("id").asLong();
        long submissionId = jdbcTemplate.queryForObject("SELECT id FROM tool_submissions WHERE tool_id = ?", Long.class, toolId);

        mockMvc.perform(post("/api/admin/reviews/{submissionId}/approve", submissionId)
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content("""
                                {"remark":"M11 联调通过"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.submissionStatus").value("APPROVED"))
                .andExpect(jsonPath("$.data.toolStatus").value("APPROVED"));

        mockMvc.perform(get("/api/tools")
                        .header("Authorization", bearer(userToken))
                        .param("mine", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(toolId));

        mockMvc.perform(post("/api/tools/{toolId}/stars", toolId)
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/tools/{toolId}/favorites", toolId)
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/tools/{toolId}/comments", toolId)
                        .header("Authorization", bearer(userToken))
                        .contentType("application/json")
                        .content("""
                                {"content":"<script>alert('x')</script>很好用"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("&lt;script&gt;alert(&#39;x&#39;)&lt;/script&gt;很好用"));

        mockMvc.perform(get("/api/tools/{toolId}", toolId)
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.starred").value(true))
                .andExpect(jsonPath("$.data.favorited").value(true))
                .andExpect(jsonPath("$.data.commentCount").value(1));

        mockMvc.perform(post("/api/admin/workflows")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content("""
                                {
                                  "name":"M11 验收工作流",
                                  "scenario":"回归验收",
                                  "description":"串联工具审核与使用流程",
                                  "steps":"提交工具，管理员审核，员工互动，再进入复盘。",
                                  "featured":true,
                                  "toolIds":[%d]
                                }
                                """.formatted(toolId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tools[0].id").value(toolId));

        mockMvc.perform(get("/api/workflows")
                        .param("featuredOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("M11 验收工作流"));

        String csv = """
                name,summary,description,url,usageGuide,tags
                M11 Imported Tool,M11 import summary,M11 import description,https://m11-imported.example.com,M11 import usage,M11 集成标签
                """;

        mockMvc.perform(post("/api/admin/import-export/imports/preview")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new com.toolshare.importexport.web.ImportCsvRequest("m11-tools.csv", csv))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.invalidRows").value(0));

        mockMvc.perform(post("/api/admin/import-export/imports")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new com.toolshare.importexport.web.ImportCsvRequest("m11-tools.csv", csv))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        mockMvc.perform(post("/api/admin/import-export/exports")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content("""
                                {"fields":["name","url","tags","updatedAt"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.content").value(org.hamcrest.Matchers.containsString("M11 Acceptance Tool")));

        mockMvc.perform(get("/api/admin/logs/audit")
                        .header("Authorization", bearer(adminToken))
                        .param("action", "TOOL_SUBMITTED")
                        .param("objectType", "TOOL")
                        .param("keyword", "M11 Acceptance Tool"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));

        mockMvc.perform(get("/api/admin/logs/operations")
                        .header("Authorization", bearer(adminToken))
                        .param("module", "TOOL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        Integer starCount = jdbcTemplate.queryForObject("SELECT star_count FROM tools WHERE id = ?", Integer.class, toolId);
        Integer favoriteCount = jdbcTemplate.queryForObject("SELECT favorite_count FROM tools WHERE id = ?", Integer.class, toolId);
        Integer commentCount = jdbcTemplate.queryForObject("SELECT comment_count FROM tools WHERE id = ?", Integer.class, toolId);
        Integer importedCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tools WHERE name = 'M11 Imported Tool'", Integer.class);
        Integer toolOperationCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM operation_logs
                WHERE module = 'TOOL'
                  AND operation = 'TOOL_SUBMIT'
                """, Integer.class);

        assertThat(starCount).isEqualTo(1);
        assertThat(favoriteCount).isEqualTo(1);
        assertThat(commentCount).isEqualTo(1);
        assertThat(importedCount).isEqualTo(1);
        assertThat(toolOperationCount).isGreaterThanOrEqualTo(1);
    }

    private void seedUser(String username, String email, String realName, String password, boolean admin) {
        Long roleId = jdbcTemplate.queryForObject("SELECT id FROM roles WHERE code = ?", Long.class, admin ? "ADMIN" : "USER");
        jdbcTemplate.update("""
                INSERT INTO users (username, email, password_hash, real_name, nickname, status, user_code, created_by, updated_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                username,
                email,
                passwordService.encode(password),
                realName,
                realName,
                "ACTIVE",
                "USR" + Math.abs(username.hashCode()),
                0L,
                0L
        );
        Long userId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = ?", Long.class, username);
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, roleId);
    }

    private String login(String username, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new com.toolshare.auth.web.AuthLoginRequest(username, password))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode payload = objectMapper.readTree(response);
        String token = payload.path("data").path("accessToken").asText();
        assertThat(token).isNotBlank();
        return token;
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
