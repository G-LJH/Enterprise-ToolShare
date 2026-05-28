package com.toolshare.log.service;

import com.toolshare.log.repository.OperationLogRepository;
import com.toolshare.log.web.AuditLogItemResponse;
import com.toolshare.log.web.OperationLogItemResponse;
import com.toolshare.log.web.PagedResponse;
import com.toolshare.user.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

@Service
public class LogManagementService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final AuditLogRepository auditLogRepository;
    private final OperationLogRepository operationLogRepository;

    public LogManagementService(AuditLogRepository auditLogRepository,
                                OperationLogRepository operationLogRepository) {
        this.auditLogRepository = auditLogRepository;
        this.operationLogRepository = operationLogRepository;
    }

    public PagedResponse<AuditLogItemResponse> listAuditLogs(String action, String objectType, String keyword, Integer page, Integer size) {
        PageRequest pageRequest = normalizePage(page, size);
        return new PagedResponse<>(
                auditLogRepository.search(action, objectType, keyword, pageRequest.size(), pageRequest.offset()).stream()
                        .map(AuditLogItemResponse::from)
                        .toList(),
                auditLogRepository.count(action, objectType, keyword),
                pageRequest.page(),
                pageRequest.size()
        );
    }

    public PagedResponse<OperationLogItemResponse> listOperationLogs(String module, Boolean success, String keyword, Integer page, Integer size) {
        PageRequest pageRequest = normalizePage(page, size);
        return new PagedResponse<>(
                operationLogRepository.search(module, success, keyword, pageRequest.size(), pageRequest.offset()).stream()
                        .map(OperationLogItemResponse::from)
                        .toList(),
                operationLogRepository.count(module, success, keyword),
                pageRequest.page(),
                pageRequest.size()
        );
    }

    private PageRequest normalizePage(Integer page, Integer size) {
        int normalizedPage = page == null ? DEFAULT_PAGE : Math.max(page, 1);
        int normalizedSize = size == null ? DEFAULT_SIZE : Math.min(Math.max(size, 1), MAX_SIZE);
        return new PageRequest(normalizedPage, normalizedSize, (normalizedPage - 1) * normalizedSize);
    }

    private record PageRequest(int page, int size, int offset) {
    }
}
