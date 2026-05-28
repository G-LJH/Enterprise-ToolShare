package com.toolshare.config;

import com.toolshare.security.CurrentUserHolder;
import com.toolshare.security.RequireAuthenticated;
import com.toolshare.security.RequireRole;
import com.toolshare.user.service.RbacContextService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.Set;

@Component
public class RoleAuthorizationInterceptor implements HandlerInterceptor {

    private final RbacContextService rbacContextService;

    public RoleAuthorizationInterceptor(RbacContextService rbacContextService) {
        this.rbacContextService = rbacContextService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        RequireAuthenticated requireAuthenticated = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), RequireAuthenticated.class);
        if (requireAuthenticated == null) {
            requireAuthenticated = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), RequireAuthenticated.class);
        }
        if (requireAuthenticated != null) {
            rbacContextService.assertAuthenticated(CurrentUserHolder.get());
        }
        RequireRole requireRole = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), RequireRole.class);
        if (requireRole == null) {
            requireRole = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), RequireRole.class);
        }
        if (requireRole == null) {
            return true;
        }
        rbacContextService.assertHasRole(CurrentUserHolder.get(), Set.copyOf(Arrays.asList(requireRole.value())));
        return true;
    }
}
