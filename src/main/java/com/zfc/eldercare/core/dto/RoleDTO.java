package com.zfc.eldercare.core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 角色新增/更新 DTO（RBAC 管理端）。
 */
public record RoleDTO(

        @NotBlank(message = "角色编码不能为空")
        @Size(max = 50, message = "角色编码不能超过 50 字")
        String roleCode,

        @NotBlank(message = "角色名称不能为空")
        @Size(max = 50, message = "角色名称不能超过 50 字")
        String roleName,

        @Size(max = 200, message = "角色描述不能超过 200 字")
        String description,

        Integer status
) {
}
