package com.toolshare.review.web;

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
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ToolReviewAdminControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordService passwordService;

    @Test
    void adminShouldReviewSubmissionAndUpdateToolStatus() throws Exception {
        Long adminId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = 'admin'", Long.class);
        Long userId = seedUser("review-submitter", "review-submitter@example.com", "Review Submitter");
        Long toolId = createPendingSubmission(userId, "Review Tool");
        Long submissionId = jdbcTemplate.queryForObject("SELECT id FROM tool_submissions WHERE tool_id = ?", Long.class, toolId);

        mockMvc.perform(get("/api/admin/reviews")
                        .header(RequestUserInterceptor.USER_ID_HEADER, adminId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].submissionId", hasItem(submissionId.intValue())))
                .andExpect(jsonPath("$.data[*].submissionStatus", hasItem("PENDING")));

        mockMvc.perform(post("/api/admin/reviews/{submissionId}/approve", submissionId)
                        .header(RequestUserInterceptor.USER_ID_HEADER, adminId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new ReviewDecisionRequest("可以上线"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.submissionStatus").value("APPROVED"))
                .andExpect(jsonPath("$.data.toolStatus").value("APPROVED"))
                .andExpect(jsonPath("$.data.remark").value("可以上线"));

        String toolStatus = jdbcTemplate.queryForObject("SELECT status FROM tools WHERE id = ?", String.class, toolId);
        String submissionStatus = jdbcTemplate.queryForObject("SELECT status FROM tool_submissions WHERE id = ?", String.class, submissionId);
        assertThat(toolStatus).isEqualTo("APPROVED");
        assertThat(submissionStatus).isEqualTo("APPROVED");
    }

    @Test
    void adminShouldRejectSubmissionAndRequireReason() throws Exception {
        Long adminId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = 'admin'", Long.class);
        Long userId = seedUser("reject-submitter", "reject-submitter@example.com", "Reject Submitter");
        Long toolId = createPendingSubmission(userId, "Reject Tool");
        Long submissionId = jdbcTemplate.queryForObject("SELECT id FROM tool_submissions WHERE tool_id = ?", Long.class, toolId);

        mockMvc.perform(post("/api/admin/reviews/{submissionId}/reject", submissionId)
                        .header(RequestUserInterceptor.USER_ID_HEADER, adminId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new ReviewDecisionRequest(" "))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("驳回原因不能为空"));

        mockMvc.perform(post("/api/admin/reviews/{submissionId}/reject", submissionId)
                        .header(RequestUserInterceptor.USER_ID_HEADER, adminId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new ReviewDecisionRequest("内容描述不足"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.submissionStatus").value("REJECTED"))
                .andExpect(jsonPath("$.data.toolStatus").value("REJECTED"))
                .andExpect(jsonPath("$.data.remark").value("内容描述不足"));
    }

    @Test
    void adminShouldManageToolReviewConfig() throws Exception {
        Long adminId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = 'admin'", Long.class);

        mockMvc.perform(get("/api/admin/reviews/config/tool-review")
                        .header(RequestUserInterceptor.USER_ID_HEADER, adminId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.toolReviewEnabled").value(true));

        mockMvc.perform(put("/api/admin/reviews/config/tool-review")
                        .header(RequestUserInterceptor.USER_ID_HEADER, adminId)
                        .contentType("application/json")
                        .content("""
                                {"toolReviewEnabled":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.toolReviewEnabled").value(false));

        String configValue = jdbcTemplate.queryForObject("""
                SELECT config_value
                FROM system_configs
                WHERE config_key = 'TOOL_REVIEW_ENABLED'
                """, String.class);
        assertThat(configValue).isEqualTo("false");
    }

    @Test
    void reviewerShouldAccessReviewApisButNotUpdateConfig() throws Exception {
        Long reviewerId = seedUser("plain-reviewer", "plain-reviewer@example.com", "Plain Reviewer", "REVIEWER");
        Long submitterId = seedUser("reviewer-submit-user", "reviewer-submit-user@example.com", "Reviewer Submit User");
        Long toolId = createPendingSubmission(submitterId, "Reviewer Tool");
        Long submissionId = jdbcTemplate.queryForObject("SELECT id FROM tool_submissions WHERE tool_id = ?", Long.class, toolId);

        mockMvc.perform(get("/api/admin/reviews")
                        .header(RequestUserInterceptor.USER_ID_HEADER, reviewerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].submissionId", hasItem(submissionId.intValue())));

        mockMvc.perform(get("/api/admin/reviews/config/tool-review")
                        .header(RequestUserInterceptor.USER_ID_HEADER, reviewerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.toolReviewEnabled").exists());

        mockMvc.perform(put("/api/admin/reviews/config/tool-review")
                        .header(RequestUserInterceptor.USER_ID_HEADER, reviewerId)
                        .contentType("application/json")
                        .content("""
                                {"toolReviewEnabled":false}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("当前账号无权访问该管理资源"));
    }

    @Test
    void nonAdminShouldNotAccessReviewApis() throws Exception {
        Long userId = seedUser("plain-review-user", "plain-review-user@example.com", "Plain Review User");

        mockMvc.perform(get("/api/admin/reviews")
                        .header(RequestUserInterceptor.USER_ID_HEADER, userId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("当前账号无权访问该管理资源"));

        mockMvc.perform(get("/api/admin/reviews/config/tool-review")
                        .header(RequestUserInterceptor.USER_ID_HEADER, userId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("当前账号无权访问该管理资源"));
    }

    private Long seedUser(String username, String email, String realName) {
        return seedUser(username, email, realName, "USER");
    }

    private Long seedUser(String username, String email, String realName, String roleCode) {
        Long userRoleId = jdbcTemplate.queryForObject("SELECT id FROM roles WHERE code = ?", Long.class, roleCode);
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

    private Long createPendingSubmission(Long submitterId, String toolName) {
        jdbcTemplate.update("""
                INSERT INTO tools (name, summary, description, url, usage_guide, recommender_id, status, star_count, favorite_count, comment_count, created_by, updated_by)
                VALUES (?, ?, ?, ?, ?, ?, 'PENDING_REVIEW', 0, 0, 0, ?, ?)
                """,
                toolName,
                toolName + " summary",
                toolName + " description",
                "https://" + toolName.toLowerCase().replace(" ", "-") + ".example.com",
                toolName + " usage",
                submitterId,
                submitterId,
                submitterId
        );
        Long toolId = jdbcTemplate.queryForObject("SELECT id FROM tools WHERE name = ?", Long.class, toolName);
        jdbcTemplate.update("""
                INSERT INTO tool_submissions (tool_id, submitter_id, status, remark, created_by, updated_by)
                VALUES (?, ?, 'PENDING', '等待管理员审核', ?, ?)
                """,
                toolId,
                submitterId,
                submitterId,
                submitterId
        );
        return toolId;
    }
}
