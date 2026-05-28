package com.toolshare.tool.service;

import com.toolshare.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class ToolStatusPolicy {

    public static final String DRAFT = "DRAFT";
    public static final String PENDING_REVIEW = "PENDING_REVIEW";
    public static final String APPROVED = "APPROVED";
    public static final String REJECTED = "REJECTED";
    public static final String OFFLINE = "OFFLINE";

    private static final Set<String> ALL_STATUSES = Set.of(DRAFT, PENDING_REVIEW, APPROVED, REJECTED, OFFLINE);

    public String normalize(String status) {
        String normalized = status == null || status.isBlank() ? APPROVED : status.trim().toUpperCase();
        if (!ALL_STATUSES.contains(normalized)) {
            throw new BadRequestException("工具状态不合法");
        }
        return normalized;
    }

    public String initialAdminStatus() {
        return APPROVED;
    }

    public String initialSubmissionStatus() {
        return PENDING_REVIEW;
    }

    public void assertCanEdit(String status) {
        String normalized = normalize(status);
        if (OFFLINE.equals(normalized)) {
            throw new BadRequestException("已下架工具不可直接编辑，请恢复后再操作");
        }
    }

    public void assertCanOffline(String status) {
        String normalized = normalize(status);
        if (OFFLINE.equals(normalized)) {
            throw new BadRequestException("工具已经处于下架状态");
        }
    }

    public boolean canReadInPublicList(String status) {
        return APPROVED.equals(normalize(status));
    }

    public boolean canReadDetail(String status, boolean ownerOrAdmin) {
        String normalized = normalize(status);
        return APPROVED.equals(normalized) || ownerOrAdmin;
    }
}
