package com.toolshare.config;

import com.toolshare.exception.BadRequestException;
import com.toolshare.security.CurrentUserHolder;
import com.toolshare.user.service.RbacContextService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RequestUserInterceptor implements HandlerInterceptor {

    public static final String USER_ID_HEADER = "X-User-Id";

    private final RbacContextService rbacContextService;

    public RequestUserInterceptor(RbacContextService rbacContextService) {
        this.rbacContextService = rbacContextService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (CurrentUserHolder.get() != null) {
            return true;
        }
        String userIdHeader = request.getHeader(USER_ID_HEADER);
        if (userIdHeader == null || userIdHeader.isBlank()) {
            return true;
        }
        Long userId;
        try {
            userId = Long.valueOf(userIdHeader);
        } catch (NumberFormatException ex) {
            throw new BadRequestException("X-User-Id 请求头必须是数字");
        }
        CurrentUserHolder.set(rbacContextService.loadCurrentUser(userId));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        CurrentUserHolder.clear();
    }
}
