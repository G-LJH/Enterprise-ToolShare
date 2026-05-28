package com.toolshare.user.web;

import com.toolshare.model.ApiResponse;
import com.toolshare.security.CurrentUserHolder;
import com.toolshare.security.RequireRole;
import com.toolshare.user.repository.RoleRepository;
import com.toolshare.user.service.UserManagementService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequireRole(RoleRepository.ADMIN)
public class UserAdminController {

    private final UserManagementService userManagementService;

    public UserAdminController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @GetMapping("/users")
    public ApiResponse<List<UserListItemResponse>> listUsers(@RequestParam(required = false) String keyword,
                                                             @RequestParam(required = false) String status,
                                                             @RequestParam(required = false) Long roleId) {
        return ApiResponse.ok(userManagementService.listUsers(keyword, status, roleId));
    }

    @GetMapping("/users/{userId}")
    public ApiResponse<UserDetailResponse> getUser(@PathVariable Long userId) {
        return ApiResponse.ok(userManagementService.getUser(userId));
    }

    @PostMapping("/users")
    public ApiResponse<UserDetailResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.ok(userManagementService.createUser(request, CurrentUserHolder.get()));
    }

    @PutMapping("/users/{userId}")
    public ApiResponse<UserDetailResponse> updateUser(@PathVariable Long userId,
                                                      @Valid @RequestBody UpdateUserRequest request) {
        return ApiResponse.ok(userManagementService.updateUser(userId, request, CurrentUserHolder.get()));
    }

    @DeleteMapping("/users/{userId}")
    public ApiResponse<Void> deleteUser(@PathVariable Long userId) {
        userManagementService.deleteUser(userId, CurrentUserHolder.get());
        return ApiResponse.ok(null);
    }

    @PostMapping("/users/{userId}/reset-password")
    public ApiResponse<Void> resetPassword(@PathVariable Long userId,
                                           @Valid @RequestBody ResetPasswordRequest request) {
        userManagementService.resetPassword(userId, request.newPassword(), CurrentUserHolder.get());
        return ApiResponse.ok(null);
    }

    @GetMapping("/roles")
    public ApiResponse<List<RoleResponse>> listRoles() {
        return ApiResponse.ok(userManagementService.listRoles().stream().map(RoleResponse::from).toList());
    }
}
