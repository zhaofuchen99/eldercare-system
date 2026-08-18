package com.zfc.eldercare.core.vo;

import com.zfc.eldercare.core.entity.Permission;

import java.time.LocalDateTime;

/**
 * 权限 VO（RBAC 管理端）。
 */
public record PermissionVO(
        Long id,
        String permissionCode,
        String permissionName,
        String description,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
    public static PermissionVO from(Permission p) {
        return new PermissionVO(p.getId(), p.getPermissionCode(), p.getPermissionName(),
                p.getDescription(), p.getCreateTime(), p.getUpdateTime());
    }
}
