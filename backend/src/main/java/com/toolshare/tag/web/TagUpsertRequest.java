package com.toolshare.tag.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TagUpsertRequest(
        @NotBlank(message = "标签名称不能为空")
        @Size(max = 64, message = "标签名称长度不能超过 64")
        String name,
        @Size(max = 256, message = "标签描述长度不能超过 256")
        String description
) {
}
