package com.toolshare.user.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateUserRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(max = 64, message = "用户名长度不能超过64")
        String username,
        @NotBlank(message = "真实姓名不能为空")
        @Size(max = 64, message = "真实姓名长度不能超过64")
        String realName,
        @Size(max = 64, message = "昵称长度不能超过64")
        String nickname,
        @NotBlank(message = "密码不能为空")
        @Size(min = 8, max = 64, message = "密码长度需在8到64之间")
        String password,
        @Pattern(regexp = "ACTIVE|DISABLED", message = "账号状态仅支持 ACTIVE 或 DISABLED")
        String status,
        @NotEmpty(message = "至少需要绑定一个角色")
        List<Long> roleIds
) {
}
