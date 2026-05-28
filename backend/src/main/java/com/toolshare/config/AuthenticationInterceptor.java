package com.toolshare.config;

import com.toolshare.auth.service.AuthService;
import com.toolshare.exception.BadRequestException;
import com.toolshare.security.CurrentUserHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthenticationInterceptor implements HandlerInterceptor {

    public static final String AUTHORIZATION_HEADER = "Authorization";
    private final AuthService authService;

    public AuthenticationInterceptor(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return true;
        }
        if (!authorizationHeader.startsWith("Bearer ")) {
            throw new BadRequestException("Authorization 请求头格式必须为 Bearer token");
        }
        String token = authorizationHeader.substring(7).trim();
        if (token.isBlank()) {
            throw new BadRequestException("Authorization token 不能为空");
        }
        CurrentUserHolder.set(authService.authenticateByToken(token));
        return true;
    }
}
