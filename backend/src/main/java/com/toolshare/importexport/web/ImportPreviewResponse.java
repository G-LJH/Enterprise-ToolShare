package com.toolshare.importexport.web;

import java.util.List;

public record ImportPreviewResponse(
        int totalRows,
        int validRows,
        int invalidRows,
        List<String> errors
) {
}
