package com.zfc.eldercare.core.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 权限资源关联表 Mapper（文档 6.3.24，无 deleted 字段，物理删）。
 */
@Mapper
public interface PermissionResourceMapper {

    /** 新增关联 */
    int insert(@Param("permissionId") Long permissionId, @Param("resourceId") Long resourceId);

    /** 查权限已挂的资源 ID 列表 */
    List<Long> selectResourceIdsByPermissionId(@Param("permissionId") Long permissionId);

    /** 批量查多个权限拥有的资源 ID 列表（RBAC 资源解析） */
    List<Long> selectResourceIdsByPermissionIds(@Param("permissionIds") List<Long> permissionIds);

    /** 删除权限全部资源关联（全量重配） */
    int deleteByPermissionId(@Param("permissionId") Long permissionId);
}
