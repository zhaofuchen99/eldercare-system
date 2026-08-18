package com.zfc.eldercare.core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 受控资源新增/更新 DTO（RBAC 管理端）。
 */
public record SysResourceDTO(

        @NotBlank(message = "资源编码不能为空")
        @Size(max = 100, message = "资源编码不能超过 100 字")
        String resourceCode,

        @NotBlank(message = "资源名称不能为空")
        @Size(max = 100, message = "资源名称不能超过 100 字")
        String resourceName,

        @NotBlank(message = "资源类型不能为空")
        @Pattern(regexp = "API|MENU|BUTTON", message = "资源类型只能是 API/MENU/BUTTON")
        String resourceType,

        @Size(max = 200, message = "资源路径不能超过 200 字")
        String path,

        Long parentId,

        Integer sortOrder
) {
}
