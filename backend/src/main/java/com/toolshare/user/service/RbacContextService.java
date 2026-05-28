package com.toolshare.user.service;

import com.toolshare.exception.ForbiddenException;
import com.toolshare.exception.UnauthorizedException;
import com.toolshare.security.CurrentUser;
import com.toolshare.user.model.RoleRecord;
import com.toolshare.user.model.UserRecord;
import com.toolshare.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class RbacContextService {

    private final UserRepository userRepository;

    public RbacContextService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public CurrentUser loadCurrentUser(Long userId) {
        UserRecord user = userRepository.findActiveById(userId)
                .orElseThrow(() -> new UnauthorizedException("当前请求未绑定有效账号"));
        if (!"ACTIVE".equals(user.status())) {
            throw new ForbiddenException("账号已停用，无法访问当前资源");
        }
        List<RoleRecord> roles = userRepository.findRolesByUserIds(List.of(user.id()))
                .getOrDefault(user.id(), List.of());
        return new CurrentUser(
                user.id(),
                user.userCode(),
                user.username(),
                user.realName(),
                user.status(),
                roles.stream().map(RoleRecord::code).collect(java.util.stream.Collectors.toSet())
        );
    }

    public void assertHasRole(CurrentUser currentUser, Set<String> requiredRoles) {
        assertAuthenticated(currentUser);
        if (!currentUser.hasAnyRole(requiredRoles)) {
            throw new ForbiddenException("当前账号无权访问该管理资源");
        }
    }

    public void assertAuthenticated(CurrentUser currentUser) {
        if (currentUser == null) {
            throw new UnauthorizedException("当前请求未登录");
        }
    }
}
