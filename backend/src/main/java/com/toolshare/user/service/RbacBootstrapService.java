package com.toolshare.user.service;

import com.toolshare.user.model.RoleRecord;
import com.toolshare.user.repository.RoleRepository;
import com.toolshare.user.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class RbacBootstrapService implements ApplicationRunner {

    private static final String DEFAULT_ADMIN_USERNAME = "admin";
    private static final String DEFAULT_ADMIN_EMAIL = "admin@space.local";
    private static final String DEFAULT_ADMIN_PASSWORD = "Admin@123456";

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final UserCodeGenerator userCodeGenerator;
    private final BCryptPasswordEncoder passwordEncoder;

    public RbacBootstrapService(RoleRepository roleRepository,
                                UserRepository userRepository,
                                UserCodeGenerator userCodeGenerator,
                                BCryptPasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.userCodeGenerator = userCodeGenerator;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureRole(RoleRepository.ADMIN, "系统管理员", "平台账号与权限管理");
        ensureRole(RoleRepository.REVIEWER, "审核员", "工具审核与配置管理");
        ensureRole(RoleRepository.USER, "普通用户", "基础浏览与互动权限");

        RoleRecord adminRole = roleRepository.findByCode(RoleRepository.ADMIN).orElseThrow();
        RoleRecord userRole = roleRepository.findByCode(RoleRepository.USER).orElseThrow();

        Optional<com.toolshare.user.model.UserRecord> existingAdmin = userRepository.findByUsername(DEFAULT_ADMIN_USERNAME);
        long adminId;
        Long operatorId = null;
        if (existingAdmin.isEmpty()) {
            adminId = userRepository.insert(
                    DEFAULT_ADMIN_USERNAME,
                    DEFAULT_ADMIN_EMAIL,
                    passwordEncoder.encode(DEFAULT_ADMIN_PASSWORD),
                    "系统管理员",
                    "Admin",
                    "ACTIVE",
                    null
            );
            operatorId = adminId;
        } else {
            adminId = existingAdmin.get().id();
            operatorId = adminId;
        }

        if (existingAdmin.map(user -> user.userCode() == null || user.userCode().isBlank()).orElse(true)) {
            userRepository.updateUserCode(adminId, userCodeGenerator.generate(adminId), operatorId);
        }

        Map<Long, List<RoleRecord>> existingRolesByUserId = userRepository.findRolesByUserIds(List.of(adminId));
        List<Long> mergedRoleIds = new ArrayList<>(existingRolesByUserId.getOrDefault(adminId, List.of())
                .stream()
                .map(RoleRecord::id)
                .collect(Collectors.toCollection(ArrayList::new)));
        if (!mergedRoleIds.contains(adminRole.id())) {
            mergedRoleIds.add(adminRole.id());
        }
        if (!mergedRoleIds.contains(userRole.id())) {
            mergedRoleIds.add(userRole.id());
        }
        userRepository.replaceRoles(adminId, mergedRoleIds);
    }

    private void ensureRole(String code, String name, String description) {
        if (roleRepository.findByCode(code).isEmpty()) {
            roleRepository.insert(code, name, description);
        }
    }
}
