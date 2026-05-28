package com.toolshare.engagement.web;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ToolEngagementControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordService passwordService;

    @Test
    void userShouldStarFavoriteAndCommentTool() throws Exception {
        Long adminId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = 'admin'", Long.class);
        Long memberId = seedUser("engaged-user", "engaged-user@example.com", "Engaged User", false);
        Long toolId = createTool(adminId, "Commentable Tool", "https://commentable.example.com");

        mockMvc.perform(post("/api/tools/{toolId}/stars", toolId)
                        .header(RequestUserInterceptor.USER_ID_HEADER, memberId))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/tools/{toolId}/favorites", toolId)
                        .header(RequestUserInterceptor.USER_ID_HEADER, memberId))
                .andExpect(status().isOk());

        String commentResponse = mockMvc.perform(post("/api/tools/{toolId}/comments", toolId)
                        .header(RequestUserInterceptor.USER_ID_HEADER, memberId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new CommentCreateRequest("<b>很实用</b>"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("&lt;b&gt;很实用&lt;/b&gt;"))
                .andExpect(jsonPath("$.data.deletable").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long commentId = objectMapper.readTree(commentResponse).path("data").path("id").asLong();

        mockMvc.perform(get("/api/tools/{toolId}", toolId)
                        .header(RequestUserInterceptor.USER_ID_HEADER, memberId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.starred").value(true))
                .andExpect(jsonPath("$.data.favorited").value(true))
                .andExpect(jsonPath("$.data.commentCount").value(1));

        mockMvc.perform(get("/api/tools/{toolId}/comments", toolId)
                        .header(RequestUserInterceptor.USER_ID_HEADER, memberId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(commentId));

        mockMvc.perform(get("/api/tools/favorites")
                        .header(RequestUserInterceptor.USER_ID_HEADER, memberId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(toolId));

        Integer starCount = jdbcTemplate.queryForObject("SELECT star_count FROM tools WHERE id = ?", Integer.class, toolId);
        Integer favoriteCount = jdbcTemplate.queryForObject("SELECT favorite_count FROM tools WHERE id = ?", Integer.class, toolId);
        Integer commentCount = jdbcTemplate.queryForObject("SELECT comment_count FROM tools WHERE id = ?", Integer.class, toolId);
        assertThat(starCount).isEqualTo(1);
        assertThat(favoriteCount).isEqualTo(1);
        assertThat(commentCount).isEqualTo(1);
    }

    @Test
    void duplicateStarShouldBeRejectedAndUnlikeShouldWork() throws Exception {
        Long adminId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = 'admin'", Long.class);
        Long memberId = seedUser("star-user", "star-user@example.com", "Star User", false);
        Long toolId = createTool(adminId, "Star Tool", "https://star.example.com");

        mockMvc.perform(post("/api/tools/{toolId}/stars", toolId)
                        .header(RequestUserInterceptor.USER_ID_HEADER, memberId))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/tools/{toolId}/stars", toolId)
                        .header(RequestUserInterceptor.USER_ID_HEADER, memberId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("当前工具已点赞，请勿重复操作"));

        mockMvc.perform(delete("/api/tools/{toolId}/stars", toolId)
                        .header(RequestUserInterceptor.USER_ID_HEADER, memberId))
                .andExpect(status().isOk());

        Integer starCount = jdbcTemplate.queryForObject("SELECT star_count FROM tools WHERE id = ?", Integer.class, toolId);
        assertThat(starCount).isEqualTo(0);
    }

    @Test
    void onlyAuthorOrAdminShouldDeleteComment() throws Exception {
        Long adminId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = 'admin'", Long.class);
        Long authorId = seedUser("comment-author", "comment-author@example.com", "Comment Author", false);
        Long otherId = seedUser("comment-other", "comment-other@example.com", "Comment Other", false);
        Long toolId = createTool(adminId, "Delete Comment Tool", "https://delete-comment.example.com");

        String payload = mockMvc.perform(post("/api/tools/{toolId}/comments", toolId)
                        .header(RequestUserInterceptor.USER_ID_HEADER, authorId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new CommentCreateRequest("评论待删除"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long commentId = objectMapper.readTree(payload).path("data").path("id").asLong();

        mockMvc.perform(delete("/api/tools/{toolId}/comments/{commentId}", toolId, commentId)
                        .header(RequestUserInterceptor.USER_ID_HEADER, otherId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("当前账号无权删除该评论"));

        mockMvc.perform(delete("/api/tools/{toolId}/comments/{commentId}", toolId, commentId)
                        .header(RequestUserInterceptor.USER_ID_HEADER, adminId))
                .andExpect(status().isOk());

        Integer commentCount = jdbcTemplate.queryForObject("SELECT comment_count FROM tools WHERE id = ?", Integer.class, toolId);
        Integer deletedFlag = jdbcTemplate.queryForObject("SELECT CASE WHEN deleted THEN 1 ELSE 0 END FROM tool_comments WHERE id = ?", Integer.class, commentId);
        Integer auditCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM audit_logs
                WHERE object_type = 'COMMENT'
                  AND object_id = ?
                """, Integer.class, String.valueOf(commentId));

        assertThat(commentCount).isEqualTo(0);
        assertThat(deletedFlag).isEqualTo(1);
        assertThat(auditCount).isEqualTo(1);
    }

    private Long seedUser(String username, String email, String realName, boolean admin) {
        Long roleId = jdbcTemplate.queryForObject("SELECT id FROM roles WHERE code = ?", Long.class, admin ? "ADMIN" : "USER");
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
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, roleId);
        return userId;
    }

    private Long createTool(Long recommenderId, String name, String url) {
        jdbcTemplate.update("""
                INSERT INTO tools (name, summary, description, url, usage_guide, recommender_id, status, star_count, favorite_count, comment_count, created_by, updated_by)
                VALUES (?, ?, ?, ?, ?, ?, 'APPROVED', 0, 0, 0, ?, ?)
                """,
                name,
                name + " summary",
                name + " description",
                url,
                name + " usage",
                recommenderId,
                recommenderId,
                recommenderId
        );
        return jdbcTemplate.queryForObject("SELECT id FROM tools WHERE name = ?", Long.class, name);
    }
}
