package com.toolshare.config;

import com.toolshare.exception.UnauthorizedException;
import com.toolshare.security.CurrentUserHolder;
import com.toolshare.security.RequireAuthenticated;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthenticationRequirementInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        RequireAuthenticated requireAuthenticated = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), RequireAuthenticated.class);
        if (requireAuthenticated == null) {
            requireAuthenticated = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), RequireAuthenticated.class);
        }
        if (requireAuthenticated == null) {
            return true;
        }
        if (CurrentUserHolder.get() == null) {
            throw new UnauthorizedException("当前请求未登录");
        }
        return true;
    }
}
