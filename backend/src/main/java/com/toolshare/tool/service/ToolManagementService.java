package com.toolshare.tool.service;

import com.toolshare.exception.BadRequestException;
import com.toolshare.exception.ConflictException;
import com.toolshare.exception.NotFoundException;
import com.toolshare.engagement.service.ToolEngagementService;
import com.toolshare.model.AuditLogEntry;
import com.toolshare.security.CurrentUser;
import com.toolshare.tag.model.TagRecord;
import com.toolshare.tag.repository.TagRepository;
import com.toolshare.tag.repository.ToolTagRepository;
import com.toolshare.tag.service.TagManagementService;
import com.toolshare.tag.service.ToolSearchPolicy;
import com.toolshare.tool.model.ToolRecord;
import com.toolshare.tool.model.ToolSubmissionRecord;
import com.toolshare.tool.repository.ToolRepository;
import com.toolshare.tool.repository.ToolSubmissionRepository;
import com.toolshare.tool.web.ToolDetailResponse;
import com.toolshare.tool.web.ToolListItemResponse;
import com.toolshare.tool.web.ToolUpsertRequest;
import com.toolshare.review.service.SubmissionStatusPolicy;
import com.toolshare.systemconfig.service.SystemConfigService;
import com.toolshare.user.repository.AuditLogRepository;
import com.toolshare.user.repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

@Service
public class ToolManagementService {

    private final ToolRepository toolRepository;
    private final ToolSubmissionRepository toolSubmissionRepository;
    private final ToolStatusPolicy toolStatusPolicy;
    private final AuditLogRepository auditLogRepository;
    private final TagManagementService tagManagementService;
    private final TagRepository tagRepository;
    private final ToolTagRepository toolTagRepository;
    private final ToolSearchPolicy toolSearchPolicy;
    private final ToolEngagementService toolEngagementService;
    private final SystemConfigService systemConfigService;

    public ToolManagementService(ToolRepository toolRepository,
                                 ToolSubmissionRepository toolSubmissionRepository,
                                 ToolStatusPolicy toolStatusPolicy,
                                 AuditLogRepository auditLogRepository,
                                 TagManagementService tagManagementService,
                                 TagRepository tagRepository,
                                 ToolTagRepository toolTagRepository,
                                 ToolSearchPolicy toolSearchPolicy,
                                 ToolEngagementService toolEngagementService,
                                 SystemConfigService systemConfigService) {
        this.toolRepository = toolRepository;
        this.toolSubmissionRepository = toolSubmissionRepository;
        this.toolStatusPolicy = toolStatusPolicy;
        this.auditLogRepository = auditLogRepository;
        this.tagManagementService = tagManagementService;
        this.tagRepository = tagRepository;
        this.toolTagRepository = toolTagRepository;
        this.toolSearchPolicy = toolSearchPolicy;
        this.toolEngagementService = toolEngagementService;
        this.systemConfigService = systemConfigService;
    }

    public List<ToolListItemResponse> listAdminTools(String keyword, String status, List<Long> tagIds, String sortBy, String sortOrder) {
        ToolSearchPolicy.ToolSearchQuery query = toolSearchPolicy.normalize(keyword, tagIds, sortBy, sortOrder);
        List<ToolRecord> tools = toolRepository.searchForAdmin(query.keyword(), status, query.tagIds(), query.sortBy(), query.sortOrder());
        return toListResponses(tools);
    }

    public List<ToolListItemResponse> listUserVisibleTools(String keyword,
                                                           boolean mine,
                                                           String status,
                                                           List<Long> tagIds,
                                                           String sortBy,
                                                           String sortOrder,
                                                           CurrentUser currentUser) {
        ToolSearchPolicy.ToolSearchQuery query = toolSearchPolicy.normalize(keyword, tagIds, sortBy, sortOrder);
        if (mine) {
            return toListResponses(toolRepository.searchByRecommender(currentUser.id(), status, query.tagIds(), query.sortBy(), query.sortOrder()));
        }
        return toListResponses(toolRepository.searchVisibleTools(query.keyword(), query.tagIds(), query.sortBy(), query.sortOrder())).stream()
                .filter(tool -> toolStatusPolicy.canReadInPublicList(tool.status()))
                .toList();
    }

    public ToolDetailResponse getAdminToolDetail(Long toolId) {
        return buildToolDetail(getToolOrThrow(toolId), null);
    }

    public ToolDetailResponse getToolDetailForUser(Long toolId, CurrentUser currentUser) {
        ToolRecord tool = getToolOrThrow(toolId);
        boolean ownerOrAdmin = tool.recommenderId().equals(currentUser.id()) || currentUser.roleCodes().contains(RoleRepository.ADMIN);
        if (!toolStatusPolicy.canReadDetail(tool.status(), ownerOrAdmin)) {
            throw new NotFoundException("工具不存在或暂不可见");
        }
        return buildToolDetail(tool, currentUser);
    }

    public List<ToolListItemResponse> listFavoriteTools(CurrentUser currentUser) {
        return toListResponses(toolRepository.findByIds(toolEngagementService.listFavoriteToolIds(currentUser.id())));
    }

    @Transactional
    public ToolDetailResponse createToolByAdmin(ToolUpsertRequest request, CurrentUser operator) {
        ValidatedToolInput input = validateInput(request);
        List<TagRecord> tags = tagManagementService.validateTagIds(request.tagIds());
        ensureNoDuplicate(input.nameLower(), input.urlLower(), null);
        long toolId = toolRepository.insert(
                input.name(),
                input.summary(),
                input.description(),
                input.url(),
                input.usageGuide(),
                operator.id(),
                toolStatusPolicy.initialAdminStatus(),
                operator.id()
        );
        toolTagRepository.replaceToolTags(toolId, tags.stream().map(TagRecord::id).toList());
        auditLogRepository.insert(new AuditLogEntry(
                "TOOL_CREATED",
                "TOOL",
                String.valueOf(toolId),
                operator.id(),
                operator.username(),
                "created tool " + input.name()
        ));
        return getAdminToolDetail(toolId);
    }

    @Transactional
    public ToolDetailResponse updateToolByAdmin(Long toolId, ToolUpsertRequest request, CurrentUser operator) {
        ToolRecord existing = getToolOrThrow(toolId);
        toolStatusPolicy.assertCanEdit(existing.status());
        ValidatedToolInput input = validateInput(request);
        List<TagRecord> tags = tagManagementService.validateTagIds(request.tagIds());
        ensureNoDuplicate(input.nameLower(), input.urlLower(), toolId);
        toolRepository.updateTool(
                toolId,
                input.name(),
                input.summary(),
                input.description(),
                input.url(),
                input.usageGuide(),
                operator.id()
        );
        toolTagRepository.replaceToolTags(toolId, tags.stream().map(TagRecord::id).toList());
        auditLogRepository.insert(new AuditLogEntry(
                "TOOL_UPDATED",
                "TOOL",
                String.valueOf(toolId),
                operator.id(),
                operator.username(),
                "updated tool " + existing.name()
        ));
        return getAdminToolDetail(toolId);
    }

    @Transactional
    public ToolDetailResponse submitTool(ToolUpsertRequest request, CurrentUser operator) {
        ValidatedToolInput input = validateInput(request);
        List<TagRecord> tags = tagManagementService.validateTagIds(request.tagIds());
        ensureNoDuplicate(input.nameLower(), input.urlLower(), null);
        boolean reviewEnabled = systemConfigService.isToolReviewEnabled();
        String toolStatus = reviewEnabled ? toolStatusPolicy.initialSubmissionStatus() : ToolStatusPolicy.APPROVED;
        String submissionStatus = reviewEnabled ? SubmissionStatusPolicy.PENDING : SubmissionStatusPolicy.APPROVED;
        String submissionRemark = reviewEnabled ? "等待管理员审核" : "审核开关关闭，系统自动通过";
        long toolId = toolRepository.insert(
                input.name(),
                input.summary(),
                input.description(),
                input.url(),
                input.usageGuide(),
                operator.id(),
                toolStatus,
                operator.id()
        );
        toolTagRepository.replaceToolTags(toolId, tags.stream().map(TagRecord::id).toList());
        toolSubmissionRepository.insert(toolId, operator.id(), submissionStatus, submissionRemark, operator.id());
        auditLogRepository.insert(new AuditLogEntry(
                "TOOL_SUBMITTED",
                "TOOL",
                String.valueOf(toolId),
                operator.id(),
                operator.username(),
                "submitted tool " + input.name() + ", reviewEnabled=" + reviewEnabled
        ));
        return getToolDetailForUser(toolId, operator);
    }

    @Transactional
    public void offlineTool(Long toolId, CurrentUser operator) {
        ToolRecord existing = getToolOrThrow(toolId);
        toolStatusPolicy.assertCanOffline(existing.status());
        toolRepository.updateStatus(toolId, ToolStatusPolicy.OFFLINE, operator.id());
        auditLogRepository.insert(new AuditLogEntry(
                "TOOL_OFFLINED",
                "TOOL",
                String.valueOf(toolId),
                operator.id(),
                operator.username(),
                "offlined tool " + existing.name()
        ));
    }

    private ToolRecord getToolOrThrow(Long toolId) {
        return toolRepository.findActiveById(toolId)
                .orElseThrow(() -> new NotFoundException("工具不存在"));
    }

    private ToolDetailResponse buildToolDetail(ToolRecord tool, CurrentUser currentUser) {
        ToolSubmissionRecord latestSubmission = toolSubmissionRepository.findLatestByToolId(tool.id()).orElse(null);
        List<TagRecord> tags = tagRepository.findByToolIds(List.of(tool.id())).getOrDefault(tool.id(), List.of());
        boolean starred = currentUser != null && toolEngagementService.hasStarred(tool.id(), currentUser.id());
        boolean favorited = currentUser != null && toolEngagementService.hasFavorited(tool.id(), currentUser.id());
        return ToolDetailResponse.from(tool, tags, latestSubmission, starred, favorited);
    }

    private ValidatedToolInput validateInput(ToolUpsertRequest request) {
        String name = normalizeRequired(request.name(), "工具名称不能为空", 256);
        String summary = normalizeOptional(request.summary(), 512);
        String description = normalizeRequired(request.description(), "工具描述不能为空", 5000);
        String url = normalizeUrl(request.url());
        String usageGuide = normalizeRequired(request.usageGuide(), "使用方法不能为空", 8000);
        return new ValidatedToolInput(name, summary, description, url, usageGuide);
    }

    private String normalizeRequired(String value, String message, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(message);
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new BadRequestException("输入内容长度超出限制");
        }
        return normalized;
    }

    private String normalizeOptional(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new BadRequestException("输入内容长度超出限制");
        }
        return normalized;
    }

    private String normalizeUrl(String url) {
        String normalized = normalizeRequired(url, "工具链接不能为空", 512);
        try {
            URI uri = new URI(normalized);
            String scheme = uri.getScheme();
            if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https")) || uri.getHost() == null) {
                throw new BadRequestException("工具链接必须为合法的 http 或 https 地址");
            }
            return normalized;
        } catch (URISyntaxException ex) {
            throw new BadRequestException("工具链接必须为合法的 http 或 https 地址");
        }
    }

    private void ensureNoDuplicate(String normalizedName, String normalizedUrl, Long excludedToolId) {
        toolRepository.findDuplicateCandidate(normalizedName, normalizedUrl, excludedToolId)
                .ifPresent(record -> {
                    throw new ConflictException("工具名称或链接已存在，请勿重复提交");
                });
    }

    private List<ToolListItemResponse> toListResponses(List<ToolRecord> tools) {
        Map<Long, List<TagRecord>> tagsByToolId = tagRepository.findByToolIds(tools.stream().map(ToolRecord::id).toList());
        return tools.stream()
                .map(tool -> ToolListItemResponse.from(tool, tagsByToolId.getOrDefault(tool.id(), List.of())))
                .toList();
    }

    private record ValidatedToolInput(
            String name,
            String summary,
            String description,
            String url,
            String usageGuide
    ) {
        String nameLower() {
            return name.toLowerCase();
        }

        String urlLower() {
            return url == null ? null : url.toLowerCase();
        }
    }
}
