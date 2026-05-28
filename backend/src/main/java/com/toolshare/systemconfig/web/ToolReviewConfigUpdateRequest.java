package com.toolshare.systemconfig.web;

import jakarta.validation.constraints.NotNull;

public record ToolReviewConfigUpdateRequest(
        @NotNull(message = "审核开关不能为空")
        Boolean toolReviewEnabled
) {
}
