package com.toolshare.tag.service;

import com.toolshare.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ToolSearchPolicyTests {

    private final ToolSearchPolicy toolSearchPolicy = new ToolSearchPolicy();

    @Test
    void shouldNormalizeSearchParamsWithWhitelistSort() {
        ToolSearchPolicy.ToolSearchQuery query = toolSearchPolicy.normalize(" figma ", List.of(3L, 2L, 3L), "starCount", "asc");
        assertThat(query.keyword()).isEqualTo("figma");
        assertThat(query.tagIds()).containsExactly(2L, 3L);
        assertThat(query.sortBy()).isEqualTo("starCount");
        assertThat(query.sortOrder()).isEqualTo("asc");
    }

    @Test
    void shouldRejectUnsupportedSortValues() {
        assertThatThrownBy(() -> toolSearchPolicy.normalize(null, List.of(), "status", "desc"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("排序字段不支持，当前仅支持 createdAt 或 starCount");

        assertThatThrownBy(() -> toolSearchPolicy.normalize(null, List.of(), "createdAt", "down"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("排序方向不支持，当前仅支持 asc 或 desc");
    }
}
