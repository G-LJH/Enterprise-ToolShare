package com.toolshare.importexport.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ImportCsvRequest(
        @NotBlank(message = "文件名不能为空")
        @Size(max = 256, message = "文件名长度不能超过 256")
        String fileName,
        @NotBlank(message = "导入内容不能为空")
        String csvContent
) {
}
