package com.zfc.eldercare.core.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 角色分配用户 DTO（PUT /api/admin/role/{id}/users，给这些用户授予该角色，替换其原角色）。
 */
public record RoleUsersDTO(
        @NotEmpty(message = "用户 ID 列表不能为空")
        List<@NotNull Long> userIds
) {
}
