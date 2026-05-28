package com.toolshare.engagement.service;

import com.toolshare.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommentSanitizerTests {

    private final CommentSanitizer sanitizer = new CommentSanitizer();

    @Test
    void shouldEscapeHtmlContent() {
        assertThat(sanitizer.sanitize("<script>alert('x')</script>"))
                .isEqualTo("&lt;script&gt;alert(&#39;x&#39;)&lt;/script&gt;");
    }

    @Test
    void shouldRejectBlankContent() {
        assertThatThrownBy(() -> sanitizer.sanitize("   "))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("评论内容不能为空");
    }
}
