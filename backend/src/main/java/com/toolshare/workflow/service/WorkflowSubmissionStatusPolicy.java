package com.toolshare.workflow.service;

import com.toolshare.exception.BadRequestException;
import org.springframework.stereotype.Service;

@Service
public class WorkflowSubmissionStatusPolicy {

    public static final String PENDING = "PENDING";
    public static final String APPROVED = "APPROVED";
    public static final String REJECTED = "REJECTED";

    public String normalize(String status) {
        if (status == null || status.isBlank()) {
            return PENDING;
        }
        String normalized = status.trim().toUpperCase();
        return switch (normalized) {
            case PENDING, APPROVED, REJECTED -> normalized;
            default -> throw new BadRequestException("工作流提交状态不合法");
        };
    }

    public String initialSubmissionStatus() {
        return PENDING;
    }

    public void assertPending(String status) {
        String normalized = normalize(status);
        if (!PENDING.equals(normalized)) {
            throw new BadRequestException("该提交单已处理，请勿重复操作");
        }
    }
}
