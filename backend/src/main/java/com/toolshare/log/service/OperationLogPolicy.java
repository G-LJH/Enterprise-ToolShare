package com.toolshare.log.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class OperationLogPolicy {

    public boolean shouldLog(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        if ("POST".equalsIgnoreCase(method) && "/api/tools/submissions".equals(path)) {
            return true;
        }
        if ("POST".equalsIgnoreCase(method) && "/api/workflows/submissions".equals(path)) {
            return true;
        }
        if (path.startsWith("/api/admin/tools")) {
            return isToolMutation(path, method);
        }
        if (path.startsWith("/api/admin/workflows")) {
            return "POST".equalsIgnoreCase(method)
                    || "PUT".equalsIgnoreCase(method)
                    || "DELETE".equalsIgnoreCase(method);
        }
        return false;
    }

    public String resolveModule(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path.startsWith("/api/admin/tools")) {
            return "TOOL";
        }
        if (path.startsWith("/api/admin/workflows")) {
            return "WORKFLOW";
        }
        if (path.startsWith("/api/tools/submissions")) {
            return "TOOL";
        }
        if (path.startsWith("/api/workflows/submissions")) {
            return "WORKFLOW";
        }
        return "ADMIN";
    }

    public String resolveOperation(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod().toUpperCase();
        if ("POST".equals(method) && "/api/tools/submissions".equals(path)) {
            return "TOOL_SUBMIT";
        }
        if ("POST".equals(method) && "/api/workflows/submissions".equals(path)) {
            return "WORKFLOW_CREATE";
        }
        if (path.startsWith("/api/admin/tools")) {
            if ("DELETE".equals(method)) {
                return "TOOL_DELETE";
            }
            if ("POST".equals(method)) {
                return "TOOL_CREATE";
            }
            if ("PUT".equals(method)) {
                return "TOOL_UPDATE";
            }
        }
        if (path.startsWith("/api/admin/workflows")) {
            if ("POST".equals(method)) {
                return "WORKFLOW_CREATE";
            }
            if ("PUT".equals(method)) {
                return "WORKFLOW_UPDATE";
            }
            if ("DELETE".equals(method)) {
                return "WORKFLOW_DELETE";
            }
        }
        return method + " " + path;
    }

    public String resolveMessage(HttpServletRequest request, int status, Exception ex) {
        StringBuilder message = new StringBuilder("status=").append(status);
        if (ex != null && ex.getMessage() != null && !ex.getMessage().isBlank()) {
            message.append(", error=").append(ex.getMessage());
        }
        return truncate(message.toString(), 500);
    }

    private String truncate(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private boolean isToolMutation(String path, String method) {
        if ("DELETE".equalsIgnoreCase(method)) {
            return true;
        }
        return "POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method);
    }
}
