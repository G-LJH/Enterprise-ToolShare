package com.toolshare.importexport.web;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ExportTaskCreateRequest(
        @NotEmpty(message = "请至少选择一个导出字段")
        List<String> fields
) {
}
