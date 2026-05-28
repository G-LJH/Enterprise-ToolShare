package com.toolshare.review.service;

import com.toolshare.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class SubmissionStatusPolicy {

    public static final String PENDING = "PENDING";
    public static final String APPROVED = "APPROVED";
    public static final String REJECTED = "REJECTED";

    private static final Set<String> ALL_STATUSES = Set.of(PENDING, APPROVED, REJECTED);

    public String normalize(String status) {
        String normalized = status == null || status.isBlank() ? PENDING : status.trim().toUpperCase();
        if (!ALL_STATUSES.contains(normalized)) {
            throw new BadRequestException("审核状态不合法");
        }
        return normalized;
    }

    public void assertPending(String status) {
        if (!PENDING.equals(normalize(status))) {
            throw new BadRequestException("当前提交单不在待审核状态");
        }
    }
}
