package com.toolshare.systemconfig.service;

import com.toolshare.exception.NotFoundException;
import com.toolshare.model.AuditLogEntry;
import com.toolshare.security.CurrentUser;
import com.toolshare.systemconfig.repository.SystemConfigRepository;
import com.toolshare.systemconfig.web.ToolReviewConfigResponse;
import com.toolshare.user.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SystemConfigService {

    public static final String TOOL_REVIEW_ENABLED_KEY = "TOOL_REVIEW_ENABLED";
    public static final String WORKFLOW_REVIEW_ENABLED_KEY = "WORKFLOW_REVIEW_ENABLED";

    private final SystemConfigRepository systemConfigRepository;
    private final AuditLogRepository auditLogRepository;

    public SystemConfigService(SystemConfigRepository systemConfigRepository,
                               AuditLogRepository auditLogRepository) {
        this.systemConfigRepository = systemConfigRepository;
        this.auditLogRepository = auditLogRepository;
    }

    public boolean isToolReviewEnabled() {
        return Boolean.parseBoolean(getConfigValueOrDefault(TOOL_REVIEW_ENABLED_KEY, "true"));
    }

    public boolean isWorkflowReviewEnabled() {
        return Boolean.parseBoolean(getConfigValueOrDefault(WORKFLOW_REVIEW_ENABLED_KEY, "true"));
    }

    public ToolReviewConfigResponse getToolReviewConfig() {
        return new ToolReviewConfigResponse(isToolReviewEnabled());
    }

    @Transactional
    public ToolReviewConfigResponse updateToolReviewEnabled(boolean enabled, CurrentUser operator) {
        ensureConfigExists(TOOL_REVIEW_ENABLED_KEY);
        systemConfigRepository.updateValue(TOOL_REVIEW_ENABLED_KEY, String.valueOf(enabled), operator.id());
        auditLogRepository.insert(new AuditLogEntry(
                "SYSTEM_CONFIG_UPDATED",
                "SYSTEM_CONFIG",
                TOOL_REVIEW_ENABLED_KEY,
                operator.id(),
                operator.username(),
                "set TOOL_REVIEW_ENABLED=" + enabled
        ));
        return new ToolReviewConfigResponse(enabled);
    }

    @Transactional
    public ToolReviewConfigResponse updateWorkflowReviewEnabled(boolean enabled, CurrentUser operator) {
        ensureConfigExists(WORKFLOW_REVIEW_ENABLED_KEY);
        systemConfigRepository.updateValue(WORKFLOW_REVIEW_ENABLED_KEY, String.valueOf(enabled), operator.id());
        auditLogRepository.insert(new AuditLogEntry(
                "SYSTEM_CONFIG_UPDATED",
                "SYSTEM_CONFIG",
                WORKFLOW_REVIEW_ENABLED_KEY,
                operator.id(),
                operator.username(),
                "set WORKFLOW_REVIEW_ENABLED=" + enabled
        ));
        return new ToolReviewConfigResponse(enabled);
    }

    private String getConfigValue(String configKey) {
        return systemConfigRepository.findActiveByKey(configKey)
                .map(SystemConfigRecord -> SystemConfigRecord.configValue() == null ? "" : SystemConfigRecord.configValue().trim())
                .orElseThrow(() -> new NotFoundException("系统配置不存在"));
    }

    private String getConfigValueOrDefault(String configKey, String defaultValue) {
        return systemConfigRepository.findActiveByKey(configKey)
                .map(SystemConfigRecord -> SystemConfigRecord.configValue() == null ? defaultValue : SystemConfigRecord.configValue().trim())
                .orElse(defaultValue);
    }

    private void ensureConfigExists(String configKey) {
        if (systemConfigRepository.findActiveByKey(configKey).isEmpty()) {
            throw new NotFoundException("系统配置不存在");
        }
    }
}
