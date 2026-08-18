package com.zfc.eldercare.core.mapper;

import com.zfc.eldercare.core.entity.Permission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 权限表 Mapper（文档 6.3.20）。
 */
@Mapper
public interface PermissionMapper {

    /** 新增权限，回填自增主键 */
    int insert(Permission permission);

    /** 按 ID 查权限（未删除） */
    Permission selectById(@Param("id") Long id);

    /** 批量查询权限（未删除），RBAC 权限解析用 */
    List<Permission> selectByIds(@Param("ids") List<Long> ids);

    /** 按权限编码查询（未删除） */
    Permission selectByCode(@Param("permissionCode") String permissionCode);

    /** 权限分页（可按关键字筛选），配合 PageHelper */
    List<Permission> selectPage(@Param("keyword") String keyword);

    /** 权限全量列表 */
    List<Permission> selectAll();

    /** 更新权限（仅更新非 null 字段） */
    int update(Permission permission);

    /** 逻辑删除 */
    int delete(@Param("id") Long id);
}
