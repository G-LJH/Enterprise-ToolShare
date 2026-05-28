package com.toolshare.tag.service;

import com.toolshare.exception.BadRequestException;
import com.toolshare.exception.ConflictException;
import com.toolshare.exception.NotFoundException;
import com.toolshare.model.AuditLogEntry;
import com.toolshare.security.CurrentUser;
import com.toolshare.tag.model.TagRecord;
import com.toolshare.tag.repository.TagRepository;
import com.toolshare.tag.repository.ToolTagRepository;
import com.toolshare.tag.web.TagResponse;
import com.toolshare.tag.web.TagUpsertRequest;
import com.toolshare.user.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TagManagementService {

    private final TagRepository tagRepository;
    private final ToolTagRepository toolTagRepository;
    private final AuditLogRepository auditLogRepository;

    public TagManagementService(TagRepository tagRepository,
                                ToolTagRepository toolTagRepository,
                                AuditLogRepository auditLogRepository) {
        this.tagRepository = tagRepository;
        this.toolTagRepository = toolTagRepository;
        this.auditLogRepository = auditLogRepository;
    }

    public List<TagResponse> listTags() {
        return tagRepository.findAllActive().stream().map(TagResponse::from).toList();
    }

    public List<TagRecord> validateTagIds(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return List.of();
        }
        List<Long> distinctTagIds = tagIds.stream().distinct().sorted().toList();
        List<TagRecord> tags = tagRepository.findByIds(distinctTagIds);
        if (tags.size() != distinctTagIds.size()) {
            throw new BadRequestException("存在无效标签，无法完成绑定");
        }
        return tags;
    }

    @Transactional
    public TagResponse createTag(TagUpsertRequest request, CurrentUser operator) {
        String name = normalizeName(request.name());
        String description = normalizeDescription(request.description());
        ensureUniqueName(name, null);
        long tagId = tagRepository.insert(name, description, operator.id());
        auditLogRepository.insert(new AuditLogEntry(
                "TAG_CREATED",
                "TAG",
                String.valueOf(tagId),
                operator.id(),
                operator.username(),
                "created tag " + name
        ));
        return TagResponse.from(findTagOrThrow(tagId));
    }

    @Transactional
    public TagResponse updateTag(Long tagId, TagUpsertRequest request, CurrentUser operator) {
        TagRecord existing = findTagOrThrow(tagId);
        String name = normalizeName(request.name());
        String description = normalizeDescription(request.description());
        ensureUniqueName(name, tagId);
        tagRepository.updateTag(tagId, name, description, operator.id());
        auditLogRepository.insert(new AuditLogEntry(
                "TAG_UPDATED",
                "TAG",
                String.valueOf(tagId),
                operator.id(),
                operator.username(),
                "updated tag " + existing.name()
        ));
        return TagResponse.from(findTagOrThrow(tagId));
    }

    @Transactional
    public void deleteTag(Long tagId, CurrentUser operator) {
        TagRecord existing = findTagOrThrow(tagId);
        tagRepository.logicalDelete(tagId, operator.id());
        toolTagRepository.deleteByTagId(tagId);
        auditLogRepository.insert(new AuditLogEntry(
                "TAG_DELETED",
                "TAG",
                String.valueOf(tagId),
                operator.id(),
                operator.username(),
                "deleted tag " + existing.name()
        ));
    }

    private TagRecord findTagOrThrow(Long tagId) {
        return tagRepository.findActiveById(tagId)
                .orElseThrow(() -> new NotFoundException("标签不存在"));
    }

    private void ensureUniqueName(String name, Long currentTagId) {
        tagRepository.findByName(name.toLowerCase())
                .filter(tag -> !tag.id().equals(currentTagId))
                .ifPresent(tag -> {
                    throw new ConflictException("标签名称已存在");
                });
    }

    private String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new BadRequestException("标签名称不能为空");
        }
        String normalized = name.trim();
        if (normalized.length() > 64) {
            throw new BadRequestException("标签名称长度不能超过 64");
        }
        return normalized;
    }

    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        String normalized = description.trim();
        if (normalized.length() > 256) {
            throw new BadRequestException("标签描述长度不能超过 256");
        }
        return normalized;
    }
}
