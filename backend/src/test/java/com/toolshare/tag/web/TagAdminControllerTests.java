package com.toolshare.tag.web;

import com.fasterxml.jackson.databind.ObjectMapper;
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
class TagAdminControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void adminShouldManageTagsAndWriteAuditLogs() throws Exception {
        Long adminId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = 'admin'", Long.class);

        String response = mockMvc.perform(post("/api/admin/tags")
                        .header(RequestUserInterceptor.USER_ID_HEADER, adminId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new TagUpsertRequest("AI 助手", "智能提效工具"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("AI 助手"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long tagId = objectMapper.readTree(response).path("data").path("id").asLong();

        mockMvc.perform(put("/api/admin/tags/{tagId}", tagId)
                        .header(RequestUserInterceptor.USER_ID_HEADER, adminId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new TagUpsertRequest("AI 平台", "升级后的标签"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("AI 平台"));

        mockMvc.perform(delete("/api/admin/tags/{tagId}", tagId)
                        .header(RequestUserInterceptor.USER_ID_HEADER, adminId))
                .andExpect(status().isOk());

        Integer deletedFlag = jdbcTemplate.queryForObject("SELECT CASE WHEN deleted THEN 1 ELSE 0 END FROM tags WHERE id = ?", Integer.class, tagId);
        assertThat(deletedFlag).isEqualTo(1);

        Integer auditCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM audit_logs
                WHERE object_type = 'TAG'
                  AND object_id = ?
                """, Integer.class, String.valueOf(tagId));
        assertThat(auditCount).isEqualTo(3);
    }

    @Test
    void listTagsShouldReturnCreatedTags() throws Exception {
        Long adminId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = 'admin'", Long.class);
        jdbcTemplate.update("""
                INSERT INTO tags (name, description, created_by, updated_by)
                VALUES (?, ?, ?, ?)
                """, "设计", "设计协作类工具", adminId, adminId);

        mockMvc.perform(get("/api/admin/tags")
                        .header(RequestUserInterceptor.USER_ID_HEADER, adminId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").isNotEmpty());
    }
}
