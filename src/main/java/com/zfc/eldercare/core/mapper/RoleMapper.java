package com.zfc.eldercare.core.mapper;

import com.zfc.eldercare.core.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色表 Mapper（文档 6.3.19）。
 */
@Mapper
public interface RoleMapper {

    /** 新增角色，回填自增主键 */
    int insert(Role role);

    /** 按 ID 查角色（未删除） */
    Role selectById(@Param("id") Long id);

    /** 批量查询角色（未删除），RBAC 权限解析用 */
    List<Role> selectByIds(@Param("ids") List<Long> ids);

    /** 按角色编码查询（未删除） */
    Role selectByCode(@Param("roleCode") String roleCode);

    /** 按角色编码查角色 ID（注册初始化 user_role 用） */
    Long selectIdByCode(@Param("roleCode") String roleCode);

    /** 角色分页（可按关键字筛选），配合 PageHelper */
    List<Role> selectPage(@Param("keyword") String keyword);

    /** 角色全量列表 */
    List<Role> selectAll();

    /** 更新角色（仅更新非 null 字段） */
    int update(Role role);

    /** 逻辑删除 */
    int delete(@Param("id") Long id);
}
