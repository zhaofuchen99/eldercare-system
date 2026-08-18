package com.zfc.eldercare.core.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 权限实体（对应 permission 表，文档 6.3.20）。
 * 权限编码格式：域:模块:操作，如 member:health:list。
 */
@Data
public class Permission {

    private Long id;

    /** 权限编码（域:模块:操作） */
    private String permissionCode;

    /** 权限名称 */
    private String permissionName;

    /** 权限描述 */
    private String description;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer deleted;
}
