package com.toolshare.tag.service;

import com.toolshare.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class ToolSearchPolicy {

    private static final Set<String> ALLOWED_SORT_BY = Set.of("createdAt", "starCount");
    private static final Set<String> ALLOWED_SORT_ORDER = Set.of("asc", "desc");

    public ToolSearchQuery normalize(String keyword, List<Long> tagIds, String sortBy, String sortOrder) {
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        List<Long> normalizedTagIds = tagIds == null ? List.of() : tagIds.stream().distinct().sorted().toList();
        String resolvedSortBy = sortBy == null || sortBy.isBlank() ? "createdAt" : sortBy.trim();
        String resolvedSortOrder = sortOrder == null || sortOrder.isBlank() ? "desc" : sortOrder.trim().toLowerCase();
        if (!ALLOWED_SORT_BY.contains(resolvedSortBy)) {
            throw new BadRequestException("排序字段不支持，当前仅支持 createdAt 或 starCount");
        }
        if (!ALLOWED_SORT_ORDER.contains(resolvedSortOrder)) {
            throw new BadRequestException("排序方向不支持，当前仅支持 asc 或 desc");
        }
        return new ToolSearchQuery(normalizedKeyword, normalizedTagIds, resolvedSortBy, resolvedSortOrder);
    }

    public record ToolSearchQuery(
            String keyword,
            List<Long> tagIds,
            String sortBy,
            String sortOrder
    ) {
    }
}
