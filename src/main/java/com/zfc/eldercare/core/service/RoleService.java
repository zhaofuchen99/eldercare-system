package com.zfc.eldercare.core.service;

import com.zfc.eldercare.core.dto.RoleDTO;
import com.zfc.eldercare.core.dto.RolePermissionDTO;
import com.zfc.eldercare.core.dto.RoleUsersDTO;
import com.zfc.eldercare.core.vo.PageVO;
import com.zfc.eldercare.core.vo.PermissionVO;
import com.zfc.eldercare.core.vo.RoleUserVO;
import com.zfc.eldercare.core.vo.RoleVO;

import java.util.List;

/**
 * 角色管理服务（RBAC 授权管理，文档 5.1）。
 */
public interface RoleService {

    /** 角色分页（可按关键字筛选） */
    PageVO<RoleVO> rolePage(String keyword, int page, int size);

    /** 角色详情 */
    RoleVO roleDetail(Long id);

    /** 新增角色 */
    Long createRole(RoleDTO dto);

    /** 更新角色（内置角色编码不可变更） */
    void updateRole(Long id, RoleDTO dto);

    /** 删除角色（内置角色不允许删除） */
    void deleteRole(Long id);

    /** 查角色已分配的权限列表 */
    List<PermissionVO> rolePermissions(Long roleId);

    /** 全量重配角色权限（role_permission） */
    void assignPermissions(Long roleId, RolePermissionDTO dto);

    /** 查该角色下的用户列表（手动分页） */
    PageVO<RoleUserVO> roleUsers(Long roleId, int page, int size);

    /** 把角色授予指定用户（替换其原角色，双写 user.role） */
    void assignUsers(Long roleId, RoleUsersDTO dto);
}
