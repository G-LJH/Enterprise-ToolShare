package com.toolshare.importexport.service;

import com.toolshare.exception.BadRequestException;
import com.toolshare.importexport.model.ExportTaskRecord;
import com.toolshare.importexport.model.ImportTaskRecord;
import com.toolshare.importexport.repository.ExportTaskRepository;
import com.toolshare.importexport.repository.ImportTaskRepository;
import com.toolshare.importexport.web.ExportTaskCreateRequest;
import com.toolshare.importexport.web.ExportTaskResponse;
import com.toolshare.importexport.web.FileDownloadResponse;
import com.toolshare.importexport.web.ImportCsvRequest;
import com.toolshare.importexport.web.ImportPreviewResponse;
import com.toolshare.importexport.web.ImportTaskResponse;
import com.toolshare.model.AuditLogEntry;
import com.toolshare.security.CurrentUser;
import com.toolshare.tag.model.TagRecord;
import com.toolshare.tag.repository.TagRepository;
import com.toolshare.tool.model.ToolRecord;
import com.toolshare.tool.repository.ToolRepository;
import com.toolshare.tool.service.ToolManagementService;
import com.toolshare.tool.web.ToolUpsertRequest;
import com.toolshare.user.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class ImportExportService {

    private static final List<String> TEMPLATE_HEADERS = List.of("name", "summary", "description", "url", "usageGuide", "tags");
    private static final Set<String> EXPORTABLE_FIELDS = Set.of(
            "name", "summary", "description", "url", "usageGuide", "recommenderName",
            "status", "starCount", "favoriteCount", "commentCount", "tags", "updatedAt"
    );

    private final ImportTaskRepository importTaskRepository;
    private final ExportTaskRepository exportTaskRepository;
    private final TagRepository tagRepository;
    private final ToolManagementService toolManagementService;
    private final ToolRepository toolRepository;
    private final AuditLogRepository auditLogRepository;

    public ImportExportService(ImportTaskRepository importTaskRepository,
                               ExportTaskRepository exportTaskRepository,
                               TagRepository tagRepository,
                               ToolManagementService toolManagementService,
                               ToolRepository toolRepository,
                               AuditLogRepository auditLogRepository) {
        this.importTaskRepository = importTaskRepository;
        this.exportTaskRepository = exportTaskRepository;
        this.tagRepository = tagRepository;
        this.toolManagementService = toolManagementService;
        this.toolRepository = toolRepository;
        this.auditLogRepository = auditLogRepository;
    }

    public FileDownloadResponse downloadTemplate() {
        String content = String.join(",", TEMPLATE_HEADERS) + "\n";
        return new FileDownloadResponse("tool-import-template.csv", content);
    }

    public ImportPreviewResponse previewImport(ImportCsvRequest request) {
        CsvParseResult parseResult = parseCsv(request.csvContent());
        List<String> errors = validateRows(parseResult.rows());
        return new ImportPreviewResponse(parseResult.rows().size(), parseResult.rows().size() - errors.size(), errors.size(), errors);
    }

    @Transactional
    public ImportTaskResponse createImportTask(ImportCsvRequest request, CurrentUser operator) {
        CsvParseResult parseResult = parseCsv(request.csvContent());
        List<String> errors = validateRows(parseResult.rows());
        if (!errors.isEmpty()) {
            ImportTaskRecord failedTask = getImportTask(importTaskRepository.insert(
                    "工具批量导入",
                    null,
                    request.fileName(),
                    "FAILED",
                    operator.id(),
                    0,
                    parseResult.rows().size(),
                    "inline://import-errors",
                    String.join(" | ", errors),
                    operator.id()
            ));
            return ImportTaskResponse.from(failedTask);
        }

        int successCount = 0;
        List<String> importErrors = new ArrayList<>();
        for (int index = 0; index < parseResult.rows().size(); index++) {
            CsvToolRow row = parseResult.rows().get(index);
            try {
                List<Long> tagIds = resolveTagIds(row.tags());
                toolManagementService.createToolByAdmin(new ToolUpsertRequest(
                        row.name(),
                        row.summary(),
                        row.description(),
                        row.url(),
                        row.usageGuide(),
                        tagIds
                ), operator);
                successCount++;
            } catch (RuntimeException ex) {
                importErrors.add("第 " + (index + 2) + " 行导入失败: " + ex.getMessage());
            }
        }

        String status = importErrors.isEmpty() ? "COMPLETED" : "PARTIAL_FAILED";
        ImportTaskRecord record = getImportTask(importTaskRepository.insert(
                "工具批量导入",
                null,
                request.fileName(),
                status,
                operator.id(),
                successCount,
                importErrors.size(),
                importErrors.isEmpty() ? null : "inline://import-errors",
                importErrors.isEmpty() ? "导入完成" : String.join(" | ", importErrors),
                operator.id()
        ));
        auditLogRepository.insert(new AuditLogEntry(
                "TOOLS_IMPORTED",
                "IMPORT_TASK",
                String.valueOf(record.id()),
                operator.id(),
                operator.username(),
                "imported tools from " + request.fileName()
        ));
        return ImportTaskResponse.from(record);
    }

    public List<ImportTaskResponse> listImportTasks() {
        return importTaskRepository.findAll().stream().map(ImportTaskResponse::from).toList();
    }

    @Transactional
    public ExportTaskResponse createExportTask(ExportTaskCreateRequest request, CurrentUser operator) {
        List<String> normalizedFields = normalizeFields(request.fields());
        List<ToolRecord> tools = toolRepository.searchForAdmin(null, null, List.of(), "createdAt", "desc");
        String csvContent = buildExportCsv(normalizedFields, tools);
        String fileName = "tool-export-" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(OffsetDateTime.now()) + ".csv";
        ExportTaskRecord record = getExportTask(exportTaskRepository.insert(
                "工具批量导出",
                "inline://exports/" + fileName,
                fileName,
                "COMPLETED",
                operator.id(),
                tools.size(),
                0,
                null,
                "导出字段: " + String.join(",", normalizedFields),
                operator.id()
        ));
        auditLogRepository.insert(new AuditLogEntry(
                "TOOLS_EXPORTED",
                "EXPORT_TASK",
                String.valueOf(record.id()),
                operator.id(),
                operator.username(),
                "exported tools fields " + String.join(",", normalizedFields)
        ));
        return ExportTaskResponse.from(record, csvContent);
    }

    public List<ExportTaskResponse> listExportTasks() {
        return exportTaskRepository.findAll().stream().map(ExportTaskResponse::from).toList();
    }

    private List<Long> resolveTagIds(String tagsValue) {
        if (tagsValue == null || tagsValue.isBlank()) {
            return List.of();
        }
        List<Long> tagIds = new ArrayList<>();
        for (String part : tagsValue.split("\\|")) {
            String normalized = part.trim().toLowerCase(Locale.ROOT);
            if (normalized.isEmpty()) {
                continue;
            }
            TagRecord tag = tagRepository.findByName(normalized)
                    .orElseThrow(() -> new BadRequestException("标签不存在: " + part.trim()));
            tagIds.add(tag.id());
        }
        return tagIds.stream().distinct().toList();
    }

    private CsvParseResult parseCsv(String csvContent) {
        String normalizedContent = csvContent == null ? "" : csvContent.replace("\r\n", "\n").trim();
        if (normalizedContent.isBlank()) {
            throw new BadRequestException("导入内容不能为空");
        }
        List<String> lines = normalizedContent.lines().toList();
        if (lines.size() < 2) {
            throw new BadRequestException("导入文件至少包含表头和一行数据");
        }
        List<String> headers = splitCsvLine(lines.get(0));
        if (!headers.equals(TEMPLATE_HEADERS)) {
            throw new BadRequestException("导入模板表头不正确");
        }
        List<CsvToolRow> rows = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            if (lines.get(i).isBlank()) {
                continue;
            }
            List<String> values = splitCsvLine(lines.get(i));
            if (values.size() != TEMPLATE_HEADERS.size()) {
                throw new BadRequestException("第 " + (i + 1) + " 行字段数量不正确");
            }
            rows.add(new CsvToolRow(
                    values.get(0).trim(),
                    blankToNull(values.get(1)),
                    values.get(2).trim(),
                    values.get(3).trim(),
                    values.get(4).trim(),
                    blankToNull(values.get(5))
            ));
        }
        return new CsvParseResult(rows);
    }

    private List<String> validateRows(List<CsvToolRow> rows) {
        List<String> errors = new ArrayList<>();
        for (int index = 0; index < rows.size(); index++) {
            CsvToolRow row = rows.get(index);
            if (row.name().isBlank()) {
                errors.add("第 " + (index + 2) + " 行工具名称不能为空");
            }
            if (row.description().isBlank()) {
                errors.add("第 " + (index + 2) + " 行工具描述不能为空");
            }
            if (row.url() != null && !row.url().isBlank()
                    && !(row.url().startsWith("http://") || row.url().startsWith("https://"))) {
                errors.add("第 " + (index + 2) + " 行工具链接必须为 http 或 https");
            }
            if (row.usageGuide().isBlank()) {
                errors.add("第 " + (index + 2) + " 行使用方法不能为空");
            }
            if (row.tags() != null && !row.tags().isBlank()) {
                for (String tagName : row.tags().split("\\|")) {
                    String normalized = tagName.trim().toLowerCase(Locale.ROOT);
                    if (!normalized.isEmpty() && tagRepository.findByName(normalized).isEmpty()) {
                        errors.add("第 " + (index + 2) + " 行存在无效标签: " + tagName.trim());
                    }
                }
            }
        }
        return errors;
    }

    private List<String> normalizeFields(List<String> fields) {
        List<String> normalized = fields.stream()
                .map(field -> field == null ? "" : field.trim())
                .filter(field -> !field.isBlank())
                .distinct()
                .toList();
        if (normalized.isEmpty()) {
            throw new BadRequestException("请至少选择一个导出字段");
        }
        if (!EXPORTABLE_FIELDS.containsAll(normalized)) {
            throw new BadRequestException("存在不支持的导出字段");
        }
        return normalized;
    }

    private String buildExportCsv(List<String> fields, List<ToolRecord> tools) {
        StringBuilder builder = new StringBuilder();
        builder.append(String.join(",", fields)).append("\n");
        Map<Long, List<TagRecord>> tagsByToolId = tagRepository.findByToolIds(tools.stream().map(ToolRecord::id).toList());
        for (ToolRecord tool : tools) {
            Map<String, String> values = new LinkedHashMap<>();
            values.put("name", escape(tool.name()));
            values.put("summary", escape(tool.summary() == null ? "" : tool.summary()));
            values.put("description", escape(tool.description()));
            values.put("url", escape(tool.url()));
            values.put("usageGuide", escape(tool.usageGuide()));
            values.put("recommenderName", escape(tool.recommenderName()));
            values.put("status", escape(tool.status()));
            values.put("starCount", String.valueOf(tool.starCount()));
            values.put("favoriteCount", String.valueOf(tool.favoriteCount()));
            values.put("commentCount", String.valueOf(tool.commentCount()));
            values.put("tags", escape(tagsByToolId.getOrDefault(tool.id(), List.of()).stream().map(TagRecord::name).reduce((a, b) -> a + "|" + b).orElse("")));
            values.put("updatedAt", escape(tool.updatedAt().toString()));
            builder.append(String.join(",", fields.stream().map(values::get).toList())).append("\n");
        }
        return builder.toString();
    }

    private String blankToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private List<String> splitCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                inQuotes = !inQuotes;
                continue;
            }
            if (ch == ',' && !inQuotes) {
                values.add(current.toString());
                current.setLength(0);
                continue;
            }
            current.append(ch);
        }
        values.add(current.toString());
        return values;
    }

    private String escape(String value) {
        String normalized = value == null ? "" : value.replace("\"", "\"\"");
        if (normalized.contains(",") || normalized.contains("\n")) {
            return "\"" + normalized + "\"";
        }
        return normalized;
    }

    private ImportTaskRecord getImportTask(long taskId) {
        return importTaskRepository.findById(taskId)
                .orElseThrow(() -> new BadRequestException("导入任务创建失败"));
    }

    private ExportTaskRecord getExportTask(long taskId) {
        return exportTaskRepository.findById(taskId)
                .orElseThrow(() -> new BadRequestException("导出任务创建失败"));
    }

    private record CsvParseResult(List<CsvToolRow> rows) {
    }

    private record CsvToolRow(
            String name,
            String summary,
            String description,
            String url,
            String usageGuide,
            String tags
    ) {
    }
}
