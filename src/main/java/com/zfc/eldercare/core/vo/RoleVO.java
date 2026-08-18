package com.zfc.eldercare.core.vo;

import com.zfc.eldercare.core.entity.Role;

import java.time.LocalDateTime;

/**
 * 角色 VO（RBAC 管理端）。
 */
public record RoleVO(
        Long id,
        String roleCode,
        String roleName,
        String description,
        Integer status,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
    public static RoleVO from(Role r) {
        return new RoleVO(r.getId(), r.getRoleCode(), r.getRoleName(), r.getDescription(),
                r.getStatus(), r.getCreateTime(), r.getUpdateTime());
    }
}
