package com.toolshare.importexport.web;

public record FileDownloadResponse(
        String fileName,
        String content
) {
}
