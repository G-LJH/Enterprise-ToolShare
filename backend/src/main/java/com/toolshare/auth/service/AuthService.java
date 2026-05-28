package com.toolshare.auth.service;

import com.toolshare.auth.config.AuthProperties;
import com.toolshare.auth.model.AuthSessionRecord;
import com.toolshare.auth.repository.AuthSessionRepository;
import com.toolshare.exception.ForbiddenException;
import com.toolshare.exception.UnauthorizedException;
import com.toolshare.model.AuditLogEntry;
import com.toolshare.security.CurrentUser;
import com.toolshare.user.model.UserRecord;
import com.toolshare.user.repository.AuditLogRepository;
import com.toolshare.user.repository.UserRepository;
import com.toolshare.user.service.RbacContextService;
import com.toolshare.auth.web.AuthLoginRequest;
import com.toolshare.auth.web.AuthLoginResponse;
import com.toolshare.auth.web.CurrentUserResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class AuthService {

    private final AuthProperties authProperties;
    private final UserRepository userRepository;
    private final AuthSessionRepository authSessionRepository;
    private final PasswordService passwordService;
    private final SessionTokenService sessionTokenService;
    private final RbacContextService rbacContextService;
    private final AuditLogRepository auditLogRepository;

    public AuthService(AuthProperties authProperties,
                       UserRepository userRepository,
                       AuthSessionRepository authSessionRepository,
                       PasswordService passwordService,
                       SessionTokenService sessionTokenService,
                       RbacContextService rbacContextService,
                       AuditLogRepository auditLogRepository) {
        this.authProperties = authProperties;
        this.userRepository = userRepository;
        this.authSessionRepository = authSessionRepository;
        this.passwordService = passwordService;
        this.sessionTokenService = sessionTokenService;
        this.rbacContextService = rbacContextService;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(noRollbackFor = UnauthorizedException.class)
    public AuthLoginResponse login(AuthLoginRequest request) {
        String username = request.username().trim();
        UserRecord user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("用户名或密码错误"));

        if (!"ACTIVE".equals(user.status())) {
            throw new ForbiddenException("账号已停用，无法登录");
        }

        OffsetDateTime now = OffsetDateTime.now();
        if (userRepository.isLoginLocked(user.id(), now)) {
            throw new ForbiddenException("连续登录失败次数过多，请稍后再试");
        }

        if (!passwordService.matches(request.password(), user.passwordHash())) {
            int failCount = userRepository.increaseLoginFailCount(user.id(), now, authProperties.loginFailMaxAttempts(), authProperties.loginLockMinutes());
            auditLogRepository.insert(new AuditLogEntry(
                    "LOGIN_FAILED",
                    "USER",
                    String.valueOf(user.id()),
                    user.id(),
                    user.username(),
                    "login failed, failCount=" + failCount
            ));
            throw new UnauthorizedException("用户名或密码错误");
        }

        userRepository.resetLoginFailCount(user.id(), now);
        String token = sessionTokenService.generateToken();
        String tokenHash = sessionTokenService.hashToken(token);
        OffsetDateTime expiresAt = now.plusHours(authProperties.sessionTtlHours());
        authSessionRepository.insert(user.id(), tokenHash, expiresAt);
        CurrentUser currentUser = rbacContextService.loadCurrentUser(user.id());
        auditLogRepository.insert(new AuditLogEntry(
                "LOGIN_SUCCEEDED",
                "USER",
                String.valueOf(user.id()),
                user.id(),
                user.username(),
                "login succeeded"
        ));
        return new AuthLoginResponse(
                token,
                expiresAt,
                CurrentUserResponse.from(currentUser)
        );
    }

    @Transactional
    public void logout(String token, CurrentUser currentUser) {
        authSessionRepository.revokeByTokenHash(sessionTokenService.hashToken(token));
        auditLogRepository.insert(new AuditLogEntry(
                "LOGOUT",
                "USER",
                String.valueOf(currentUser.id()),
                currentUser.id(),
                currentUser.username(),
                "logout succeeded"
        ));
    }

    public CurrentUserResponse getCurrentUser(CurrentUser currentUser) {
        if (currentUser == null) {
            throw new UnauthorizedException("当前请求未登录");
        }
        return CurrentUserResponse.from(currentUser);
    }

    public CurrentUser authenticateByToken(String token) {
        String tokenHash = sessionTokenService.hashToken(token);
        AuthSessionRecord session = authSessionRepository.findActiveByTokenHash(tokenHash, OffsetDateTime.now())
                .orElseThrow(() -> new UnauthorizedException("登录态已失效，请重新登录"));
        authSessionRepository.touch(session.id());
        return rbacContextService.loadCurrentUser(session.userId());
    }
}
