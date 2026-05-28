package com.toolshare.tool.service;

import com.toolshare.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ToolStatusPolicyTests {

    private final ToolStatusPolicy toolStatusPolicy = new ToolStatusPolicy();

    @Test
    void shouldNormalizeAndValidateKnownStatuses() {
        assertThat(toolStatusPolicy.normalize("approved")).isEqualTo(ToolStatusPolicy.APPROVED);
        assertThat(toolStatusPolicy.initialAdminStatus()).isEqualTo(ToolStatusPolicy.APPROVED);
        assertThat(toolStatusPolicy.initialSubmissionStatus()).isEqualTo(ToolStatusPolicy.PENDING_REVIEW);
        assertThat(toolStatusPolicy.canReadDetail(ToolStatusPolicy.APPROVED, false)).isTrue();
        assertThat(toolStatusPolicy.canReadDetail(ToolStatusPolicy.PENDING_REVIEW, true)).isTrue();
    }

    @Test
    void shouldRejectIllegalStatusOperations() {
        assertThatThrownBy(() -> toolStatusPolicy.normalize("unknown"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("工具状态不合法");

        assertThatThrownBy(() -> toolStatusPolicy.assertCanEdit(ToolStatusPolicy.OFFLINE))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("已下架工具不可直接编辑，请恢复后再操作");

        assertThatThrownBy(() -> toolStatusPolicy.assertCanOffline(ToolStatusPolicy.OFFLINE))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("工具已经处于下架状态");
    }
}
