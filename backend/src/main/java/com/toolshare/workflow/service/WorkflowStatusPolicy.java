package com.toolshare.workflow.service;

import com.toolshare.exception.BadRequestException;
import org.springframework.stereotype.Service;

@Service
public class WorkflowStatusPolicy {

    public static final String PENDING_REVIEW = "PENDING_REVIEW";
    public static final String APPROVED = "APPROVED";
    public static final String REJECTED = "REJECTED";
    public static final String OFFLINE = "OFFLINE";

    public String normalize(String status) {
        if (status == null || status.isBlank()) {
            return PENDING_REVIEW;
        }
        String normalized = status.trim().toUpperCase();
        return switch (normalized) {
            case PENDING_REVIEW, APPROVED, REJECTED, OFFLINE -> normalized;
            default -> throw new BadRequestException("工作流状态不合法");
        };
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
            throw new BadRequestException("已下架工作流不可直接编辑，请恢复后再操作");
        }
    }

    public void assertCanOffline(String status) {
        String normalized = normalize(status);
        if (OFFLINE.equals(normalized)) {
            throw new BadRequestException("工作流已经处于下架状态");
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
