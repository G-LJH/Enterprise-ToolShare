package com.toolshare.engagement.service;

import com.toolshare.exception.BadRequestException;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

@Component
public class CommentSanitizer {

    public String sanitize(String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            throw new BadRequestException("评论内容不能为空");
        }
        String normalized = rawContent.trim();
        if (normalized.length() > 2000) {
            throw new BadRequestException("评论内容长度不能超过 2000");
        }
        return HtmlUtils.htmlEscape(normalized);
    }
}
