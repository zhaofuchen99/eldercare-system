package com.zfc.eldercare.core.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 受控资源实体（对应 resource 表，文档 6.3.21）。
 * resourceType 三类：API 接口路径级（接口访问控制）/ MENU 菜单（前端菜单显隐）/ BUTTON 按钮（页面按钮显隐）。
 */
@Data
public class SysResource {

    private Long id;

    /** 资源编码，如 api:member:health / menu:admin:dashboard / btn:assessment:publish */
    private String resourceCode;

    /** 资源名称 */
    private String resourceName;

    /** 资源类型：API/MENU/BUTTON */
    private String resourceType;

    /** 接口类型为接口路径模式（如 /api/member/health/**）；菜单/按钮可为空 */
    private String path;

    /** 父资源 ID（菜单树形结构），根为 0 */
    private Long parentId;

    /** 排序号 */
    private Integer sortOrder;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer deleted;
}
