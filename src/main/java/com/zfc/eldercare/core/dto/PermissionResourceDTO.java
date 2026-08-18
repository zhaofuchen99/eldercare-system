package com.zfc.eldercare.core.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 权限资源关联 DTO（PUT /api/admin/permission/{id}/resources，全量重配）。
 */
public record PermissionResourceDTO(
        @NotEmpty(message = "资源 ID 列表不能为空")
        List<@NotNull Long> resourceIds
) {
}
