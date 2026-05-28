package com.toolshare.auth.web;

import com.toolshare.auth.service.AuthService;
import com.toolshare.exception.UnauthorizedException;
import com.toolshare.model.ApiResponse;
import com.toolshare.security.CurrentUserHolder;
import com.toolshare.security.RequireAuthenticated;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<AuthLoginResponse> login(@Valid @RequestBody AuthLoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @PostMapping("/logout")
    @RequireAuthenticated
    public ApiResponse<Void> logout(@RequestHeader(name = AUTHORIZATION_HEADER, required = false) String authorizationHeader) {
        String token = extractBearerToken(authorizationHeader);
        authService.logout(token, CurrentUserHolder.get());
        return ApiResponse.ok(null);
    }

    @GetMapping("/me")
    @RequireAuthenticated
    public ApiResponse<CurrentUserResponse> currentUser() {
        return ApiResponse.ok(authService.getCurrentUser(CurrentUserHolder.get()));
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("当前请求未登录");
        }
        return authorizationHeader.substring(7).trim();
    }
}
