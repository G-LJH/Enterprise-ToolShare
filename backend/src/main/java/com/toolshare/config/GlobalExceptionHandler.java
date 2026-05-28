package com.toolshare.config;

import com.toolshare.exception.BadRequestException;
import com.toolshare.exception.ConflictException;
import com.toolshare.exception.ForbiddenException;
import com.toolshare.exception.NotFoundException;
import com.toolshare.exception.UnauthorizedException;
import com.toolshare.model.ApiResponse;
import com.toolshare.security.CurrentUser;
import com.toolshare.security.CurrentUserHolder;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationException(MethodArgumentNotValidException ex,
                                                                         HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("validation failed: {}, {}", requestSummary(request), message);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadRequest(BadRequestException ex,
                                                                HttpServletRequest request) {
        log.warn("bad request: {}, {}", requestSummary(request), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Object>> handleUnauthorized(UnauthorizedException ex,
                                                                  HttpServletRequest request) {
        log.warn("unauthorized request: {}, {}", requestSummary(request), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResponse<Object>> handleForbidden(ForbiddenException ex,
                                                               HttpServletRequest request) {
        log.warn("forbidden request: {}, {}", requestSummary(request), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNotFound(NotFoundException ex,
                                                              HttpServletRequest request) {
        log.warn("resource not found: {}, {}", requestSummary(request), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiResponse<Object>> handleConflict(ConflictException ex,
                                                              HttpServletRequest request) {
        log.warn("conflict request: {}, {}", requestSummary(request), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(Exception ex,
                                                                      HttpServletRequest request) {
        log.error("unexpected server error, {}", requestSummary(request), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("内部服务错误"));
    }

    private String requestSummary(HttpServletRequest request) {
        CurrentUser currentUser = CurrentUserHolder.get();
        String query = request.getQueryString();
        String querySuffix = query == null || query.isBlank() ? "" : "?" + query;
        String user = currentUser == null ? "anonymous" : currentUser.username() + "#" + currentUser.id();
        return "method=%s, path=%s%s, user=%s".formatted(
                request.getMethod(),
                request.getRequestURI(),
                querySuffix,
                user
        );
    }
}
