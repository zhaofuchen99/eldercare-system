package com.zfc.eldercare.core.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色权限关联表 Mapper（文档 6.3.23，无 deleted 字段，物理删）。
 */
@Mapper
public interface RolePermissionMapper {

    /** 新增关联 */
    int insert(@Param("roleId") Long roleId, @Param("permissionId") Long permissionId);

    /** 查角色已分配的权限 ID 列表 */
    List<Long> selectPermissionIdsByRoleId(@Param("roleId") Long roleId);

    /** 批量查多个角色拥有的权限 ID 列表（RBAC 权限解析） */
    List<Long> selectPermissionIdsByRoleIds(@Param("roleIds") List<Long> roleIds);

    /** 删除角色全部权限关联（全量重配） */
    int deleteByRoleId(@Param("roleId") Long roleId);
}
