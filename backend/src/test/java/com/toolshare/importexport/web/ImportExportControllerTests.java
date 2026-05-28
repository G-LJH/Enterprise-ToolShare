package com.toolshare.importexport.web;

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
class ImportExportControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordService passwordService;

    @Test
    void adminShouldDownloadTemplatePreviewImportAndExportTools() throws Exception {
        Long adminId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = 'admin'", Long.class);
        Long aiTagId = seedTag(adminId, "AI 导出");
        createTool(adminId, "Export Tool", "https://export-tool.example.com", aiTagId);

        mockMvc.perform(get("/api/admin/import-export/template")
                        .header(RequestUserInterceptor.USER_ID_HEADER, adminId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fileName").value("tool-import-template.csv"))
                .andExpect(jsonPath("$.data.content").value("name,summary,description,url,usageGuide,tags\n"));

        String csv = """
                name,summary,description,url,usageGuide,tags
                Imported Tool,一句话摘要,导入描述,https://imported-tool.example.com,导入使用方法,AI 导出
                """;

        mockMvc.perform(post("/api/admin/import-export/imports/preview")
                        .header(RequestUserInterceptor.USER_ID_HEADER, adminId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new ImportCsvRequest("tools.csv", csv))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRows").value(1))
                .andExpect(jsonPath("$.data.invalidRows").value(0));

        mockMvc.perform(post("/api/admin/import-export/imports")
                        .header(RequestUserInterceptor.USER_ID_HEADER, adminId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new ImportCsvRequest("tools.csv", csv))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.successCount").value(1));

        Integer importedCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tools WHERE name = 'Imported Tool'", Integer.class);
        assertThat(importedCount).isEqualTo(1);

        mockMvc.perform(post("/api/admin/import-export/exports")
                        .header(RequestUserInterceptor.USER_ID_HEADER, adminId)
                        .contentType("application/json")
                        .content("""
                                {"fields":["name","url","tags"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.content").exists());
    }

    @Test
    void nonAdminShouldNotAccessImportExportApis() throws Exception {
        Long userId = seedUser("import-user", "import-user@example.com", "Import User");

        mockMvc.perform(get("/api/admin/import-export/template")
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

    private void createTool(Long operatorId, String name, String url, Long tagId) {
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
        Long toolId = jdbcTemplate.queryForObject("SELECT id FROM tools WHERE name = ?", Long.class, name);
        jdbcTemplate.update("INSERT INTO tool_tags (tool_id, tag_id) VALUES (?, ?)", toolId, tagId);
    }
}
