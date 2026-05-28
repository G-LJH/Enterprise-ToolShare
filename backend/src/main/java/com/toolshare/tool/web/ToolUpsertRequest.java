package com.toolshare.tool.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ToolUpsertRequest(
        @NotBlank(message = "工具名称不能为空")
        @Size(max = 256, message = "工具名称长度不能超过 256")
        String name,
        @Size(max = 512, message = "工具摘要长度不能超过 512")
        String summary,
        @NotBlank(message = "工具描述不能为空")
        String description,
        @NotBlank(message = "工具链接不能为空")
        @Size(max = 512, message = "工具链接长度不能超过 512")
        String url,
        @NotBlank(message = "使用方法不能为空")
        String usageGuide,
        List<Long> tagIds
) {
}
