package com.toolshare.log.web;

import com.toolshare.model.ApiResponse;
import com.toolshare.security.RequireRole;
import com.toolshare.log.service.LogManagementService;
import com.toolshare.user.repository.RoleRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/logs")
@RequireRole(RoleRepository.ADMIN)
public class AdminLogController {

    private final LogManagementService logManagementService;

    public AdminLogController(LogManagementService logManagementService) {
        this.logManagementService = logManagementService;
    }

    @GetMapping("/audit")
    public ApiResponse<PagedResponse<AuditLogItemResponse>> listAuditLogs(@RequestParam(required = false) String action,
                                                                          @RequestParam(required = false) String objectType,
                                                                          @RequestParam(required = false) String keyword,
                                                                          @RequestParam(required = false) Integer page,
                                                                          @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(logManagementService.listAuditLogs(action, objectType, keyword, page, size));
    }

    @GetMapping("/operations")
    public ApiResponse<PagedResponse<OperationLogItemResponse>> listOperationLogs(@RequestParam(required = false) String module,
                                                                                  @RequestParam(required = false) Boolean success,
                                                                                  @RequestParam(required = false) String keyword,
                                                                                  @RequestParam(required = false) Integer page,
                                                                                  @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(logManagementService.listOperationLogs(module, success, keyword, page, size));
    }
}
