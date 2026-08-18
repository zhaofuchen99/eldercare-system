package com.zfc.eldercare.core.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户角色关联实体（对应 user_role 表，文档 6.3.22）。
 * 用户与角色多对多关联（user.role 为冗余字段，与关联表保持一致）。
 */
@Data
public class UserRole {

    private Long id;

    /** 用户 ID（关联 user.id） */
    private Long userId;

    /** 角色 ID（关联 role.id） */
    private Long roleId;

    private LocalDateTime createTime;
}
