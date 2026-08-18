package com.zfc.eldercare.core.mapper;

import com.zfc.eldercare.core.entity.SysResource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 受控资源表 Mapper（文档 6.3.21）。
 */
@Mapper
public interface SysResourceMapper {

    /** 新增资源，回填自增主键 */
    int insert(SysResource resource);

    /** 按 ID 查资源（未删除） */
    SysResource selectById(@Param("id") Long id);

    /** 批量查询资源（未删除），RBAC 资源解析用 */
    List<SysResource> selectByIds(@Param("ids") List<Long> ids);

    /** 按资源编码查询（未删除） */
    SysResource selectByCode(@Param("resourceCode") String resourceCode);

    /** 资源分页（可按类型/关键字筛选），配合 PageHelper */
    List<SysResource> selectPage(@Param("type") String type, @Param("keyword") String keyword);

    /** 按类型查全部资源（菜单树组装用） */
    List<SysResource> selectByType(@Param("type") String type);

    /** 资源全量列表 */
    List<SysResource> selectAll();

    /** 更新资源（仅更新非 null 字段） */
    int update(SysResource resource);

    /** 逻辑删除 */
    int delete(@Param("id") Long id);
}
