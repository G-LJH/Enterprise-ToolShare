package com.toolshare.user.service;

import com.toolshare.auth.service.PasswordService;
import com.toolshare.exception.BadRequestException;
import com.toolshare.exception.ConflictException;
import com.toolshare.exception.NotFoundException;
import com.toolshare.model.AuditLogEntry;
import com.toolshare.security.CurrentUser;
import com.toolshare.user.model.RoleRecord;
import com.toolshare.user.model.UserRecord;
import com.toolshare.user.repository.AuditLogRepository;
import com.toolshare.user.repository.RoleRepository;
import com.toolshare.user.repository.UserRepository;
import com.toolshare.user.web.CreateUserRequest;
import com.toolshare.user.web.UpdateUserRequest;
import com.toolshare.user.web.UserDetailResponse;
import com.toolshare.user.web.UserListItemResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class UserManagementService {

    private static final String INTERNAL_EMAIL_DOMAIN = "tool-share.local";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordService passwordService;
    private final UserCodeGenerator userCodeGenerator;

    public UserManagementService(UserRepository userRepository,
                                 RoleRepository roleRepository,
                                 AuditLogRepository auditLogRepository,
                                 PasswordService passwordService,
                                 UserCodeGenerator userCodeGenerator) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.auditLogRepository = auditLogRepository;
        this.passwordService = passwordService;
        this.userCodeGenerator = userCodeGenerator;
    }

    public List<UserListItemResponse> listUsers(String keyword, String status, Long roleId) {
        List<UserRecord> users = userRepository.searchUsers(keyword, status, roleId);
        Map<Long, List<RoleRecord>> rolesByUserId = userRepository.findRolesByUserIds(users.stream().map(UserRecord::id).toList());
        return users.stream()
                .map(user -> UserListItemResponse.from(user, rolesByUserId.getOrDefault(user.id(), List.of())))
                .toList();
    }

    public UserDetailResponse getUser(Long userId) {
        UserRecord user = userRepository.findActiveById(userId)
                .orElseThrow(() -> new NotFoundException("账号不存在"));
        List<RoleRecord> roles = userRepository.findRolesByUserIds(List.of(userId)).getOrDefault(userId, List.of());
        return UserDetailResponse.from(user, roles);
    }

    public List<RoleRecord> listRoles() {
        return roleRepository.findAllActive();
    }

    @Transactional
    public UserDetailResponse createUser(CreateUserRequest request, CurrentUser operator) {
        List<RoleRecord> roles = enforceFixedAdminRoles(validateRoles(request.roleIds()));
        String username = normalizeUsername(request.username());
        ensureUniqueUsername(username, null);

        long userId = userRepository.insert(
                username,
                buildInternalEmail(username),
                passwordService.encode(request.password()),
                request.realName().trim(),
                normalizeNickname(request.nickname()),
                normalizeStatus(request.status()),
                operator.id()
        );
        userRepository.updateUserCode(userId, userCodeGenerator.generate(userId), operator.id());
        userRepository.replaceRoles(userId, roles.stream().map(RoleRecord::id).toList());

        auditLogRepository.insert(new AuditLogEntry(
                "USER_CREATED",
                "USER",
                String.valueOf(userId),
                operator.id(),
                operator.username(),
                "created user " + username
        ));
        return getUser(userId);
    }

    @Transactional
    public UserDetailResponse updateUser(Long userId, UpdateUserRequest request, CurrentUser operator) {
        UserRecord existingUser = userRepository.findActiveById(userId)
                .orElseThrow(() -> new NotFoundException("账号不存在"));
        if (existingUser.id().equals(operator.id()) && "DISABLED".equals(normalizeStatus(request.status()))) {
            throw new BadRequestException("不能停用当前登录的管理员账号");
        }
        List<RoleRecord> existingRoles = userRepository.findRolesByUserIds(List.of(userId)).getOrDefault(userId, List.of());
        List<RoleRecord> roles = enforceUpdateRoles(existingRoles, validateRoles(request.roleIds()));

        userRepository.updateUser(
                userId,
                request.realName().trim(),
                normalizeNickname(request.nickname()),
                normalizeStatus(request.status()),
                operator.id()
        );
        userRepository.replaceRoles(userId, roles.stream().map(RoleRecord::id).toList());

        auditLogRepository.insert(new AuditLogEntry(
                "USER_UPDATED",
                "USER",
                String.valueOf(userId),
                operator.id(),
                operator.username(),
                "updated user " + existingUser.username()
        ));
        return getUser(userId);
    }

    @Transactional
    public void deleteUser(Long userId, CurrentUser operator) {
        UserRecord existingUser = userRepository.findActiveById(userId)
                .orElseThrow(() -> new NotFoundException("账号不存在"));
        List<RoleRecord> existingRoles = userRepository.findRolesByUserIds(List.of(userId)).getOrDefault(userId, List.of());
        if (existingUser.id().equals(operator.id())) {
            throw new BadRequestException("不能删除当前登录的管理员账号");
        }
        if (containsAdminRole(existingRoles)) {
            throw new BadRequestException("管理员账号不能删除");
        }
        userRepository.logicalDelete(userId, operator.id());
        userRepository.replaceRoles(userId, List.of());
        auditLogRepository.insert(new AuditLogEntry(
                "USER_DELETED",
                "USER",
                String.valueOf(userId),
                operator.id(),
                operator.username(),
                "deleted user " + existingUser.username()
        ));
    }

    @Transactional
    public void resetPassword(Long userId, String newPassword, CurrentUser operator) {
        UserRecord existingUser = userRepository.findActiveById(userId)
                .orElseThrow(() -> new NotFoundException("账号不存在"));
        userRepository.updatePassword(userId, passwordService.encode(newPassword), operator.id());
        auditLogRepository.insert(new AuditLogEntry(
                "USER_PASSWORD_RESET",
                "USER",
                String.valueOf(userId),
                operator.id(),
                operator.username(),
                "reset password for user " + existingUser.username()
        ));
    }

    private List<RoleRecord> validateRoles(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            throw new BadRequestException("至少需要绑定一个角色");
        }
        List<Long> distinctRoleIds = roleIds.stream().distinct().sorted(Comparator.naturalOrder()).toList();
        List<RoleRecord> roles = roleRepository.findByIds(distinctRoleIds);
        if (roles.size() != distinctRoleIds.size()) {
            throw new BadRequestException("存在无效角色，无法完成绑定");
        }
        return roles;
    }

    private List<RoleRecord> enforceFixedAdminRoles(List<RoleRecord> roles) {
        if (!containsAdminRole(roles)) {
            return roles;
        }
        return roles.stream()
                .filter(role -> RoleRepository.ADMIN.equals(role.code()))
                .toList();
    }

    private List<RoleRecord> enforceUpdateRoles(List<RoleRecord> existingRoles, List<RoleRecord> requestedRoles) {
        if (!containsAdminRole(existingRoles)) {
            return enforceFixedAdminRoles(requestedRoles);
        }
        if (!containsAdminRole(requestedRoles)) {
            throw new BadRequestException("管理员权限固定，不能移除 ADMIN 角色");
        }
        return existingRoles.stream()
                .filter(role -> RoleRepository.ADMIN.equals(role.code()))
                .toList();
    }

    private boolean containsAdminRole(List<RoleRecord> roles) {
        return roles.stream().anyMatch(role -> RoleRepository.ADMIN.equals(role.code()));
    }

    private void ensureUniqueUsername(String username, Long currentUserId) {
        userRepository.findByUsername(username)
                .filter(user -> !user.id().equals(currentUserId))
                .ifPresent(user -> {
                    throw new ConflictException("用户名已存在");
                });
    }

    private String normalizeUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new BadRequestException("用户名不能为空");
        }
        return username.trim();
    }

    private String normalizeNickname(String nickname) {
        return nickname == null ? null : nickname.trim();
    }

    private String normalizeStatus(String status) {
        String normalized = status == null || status.isBlank() ? "ACTIVE" : status.trim().toUpperCase();
        if (!Set.of("ACTIVE", "DISABLED").contains(normalized)) {
            throw new BadRequestException("账号状态仅支持 ACTIVE 或 DISABLED");
        }
        return normalized;
    }

    private String buildInternalEmail(String username) {
        return username.trim().toLowerCase() + "@" + INTERNAL_EMAIL_DOMAIN;
    }
}
