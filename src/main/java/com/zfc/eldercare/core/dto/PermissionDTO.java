package com.zfc.eldercare.core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 权限新增/更新 DTO（RBAC 管理端）。
 */
public record PermissionDTO(

        @NotBlank(message = "权限编码不能为空")
        @Size(max = 100, message = "权限编码不能超过 100 字")
        String permissionCode,

        @NotBlank(message = "权限名称不能为空")
        @Size(max = 100, message = "权限名称不能超过 100 字")
        String permissionName,

        @Size(max = 200, message = "权限描述不能超过 200 字")
        String description
) {
}
