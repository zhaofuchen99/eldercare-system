package com.zfc.eldercare.core.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 角色权限分配 DTO（PUT /api/admin/role/{id}/permissions，全量重配）。
 */
public record RolePermissionDTO(
        @NotEmpty(message = "权限 ID 列表不能为空")
        List<@NotNull Long> permissionIds
) {
}
