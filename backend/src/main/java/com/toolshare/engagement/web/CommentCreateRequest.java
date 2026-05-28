package com.toolshare.engagement.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentCreateRequest(
        @NotBlank(message = "评论内容不能为空")
        @Size(max = 2000, message = "评论内容长度不能超过 2000")
        String content
) {
}
