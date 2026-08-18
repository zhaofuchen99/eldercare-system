package com.zfc.eldercare.core.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色实体（对应 role 表，文档 6.3.19）。
 */
@Data
public class Role {

    private Long id;

    /** 角色编码：MEMBER会员/ADMIN管理员 */
    private String roleCode;

    /** 角色名称 */
    private String roleName;

    /** 角色描述 */
    private String description;

    /** 状态：1 启用/0 停用 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer deleted;
}
