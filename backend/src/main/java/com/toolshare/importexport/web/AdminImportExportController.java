package com.toolshare.importexport.web;

import com.toolshare.importexport.service.ImportExportService;
import com.toolshare.model.ApiResponse;
import com.toolshare.security.CurrentUserHolder;
import com.toolshare.security.RequireRole;
import com.toolshare.user.repository.RoleRepository;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/import-export")
@RequireRole(RoleRepository.ADMIN)
public class AdminImportExportController {

    private final ImportExportService importExportService;

    public AdminImportExportController(ImportExportService importExportService) {
        this.importExportService = importExportService;
    }

    @GetMapping("/template")
    public ApiResponse<FileDownloadResponse> downloadTemplate() {
        return ApiResponse.ok(importExportService.downloadTemplate());
    }

    @PostMapping("/imports/preview")
    public ApiResponse<ImportPreviewResponse> previewImport(@Valid @RequestBody ImportCsvRequest request) {
        return ApiResponse.ok(importExportService.previewImport(request));
    }

    @PostMapping("/imports")
    public ApiResponse<ImportTaskResponse> createImportTask(@Valid @RequestBody ImportCsvRequest request) {
        return ApiResponse.ok(importExportService.createImportTask(request, CurrentUserHolder.get()));
    }

    @GetMapping("/imports")
    public ApiResponse<List<ImportTaskResponse>> listImportTasks() {
        return ApiResponse.ok(importExportService.listImportTasks());
    }

    @PostMapping("/exports")
    public ApiResponse<ExportTaskResponse> createExportTask(@Valid @RequestBody ExportTaskCreateRequest request) {
        return ApiResponse.ok(importExportService.createExportTask(request, CurrentUserHolder.get()));
    }

    @GetMapping("/exports")
    public ApiResponse<List<ExportTaskResponse>> listExportTasks() {
        return ApiResponse.ok(importExportService.listExportTasks());
    }
}
