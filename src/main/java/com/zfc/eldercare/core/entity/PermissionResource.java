package com.zfc.eldercare.core.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 权限资源关联实体（对应 permission_resource 表，文档 6.3.24）。
 */
@Data
public class PermissionResource {

    private Long id;

    /** 权限 ID（关联 permission.id） */
    private Long permissionId;

    /** 资源 ID（关联 resource.id） */
    private Long resourceId;

    private LocalDateTime createTime;
}
