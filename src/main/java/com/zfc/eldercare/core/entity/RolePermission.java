package com.zfc.eldercare.core.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色权限关联实体（对应 role_permission 表，文档 6.3.23）。
 */
@Data
public class RolePermission {

    private Long id;

    /** 角色 ID（关联 role.id） */
    private Long roleId;

    /** 权限 ID（关联 permission.id） */
    private Long permissionId;

    private LocalDateTime createTime;
}
