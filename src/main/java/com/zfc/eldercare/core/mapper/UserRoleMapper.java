package com.zfc.eldercare.core.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户角色关联表 Mapper（文档 6.3.22，无 deleted 字段，物理删）。
 */
@Mapper
public interface UserRoleMapper {

    /** 新增关联 */
    int insert(@Param("userId") Long userId, @Param("roleId") Long roleId);

    /** 查用户已分配的角色 ID（RBAC 权限解析） */
    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);

    /** 查该角色下的用户 ID 列表（管理端查看/分配） */
    List<Long> selectUserIdsByRoleId(@Param("roleId") Long roleId);

    /** 删除用户全部角色关联（用户换角色用） */
    int deleteByUserId(@Param("userId") Long userId);

    /** 删除角色全部用户关联 */
    int deleteByRoleId(@Param("roleId") Long roleId);
}
