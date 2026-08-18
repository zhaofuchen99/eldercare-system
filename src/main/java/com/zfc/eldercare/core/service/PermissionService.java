package com.zfc.eldercare.core.service;

import com.zfc.eldercare.core.dto.PermissionDTO;
import com.zfc.eldercare.core.dto.PermissionResourceDTO;
import com.zfc.eldercare.core.vo.PageVO;
import com.zfc.eldercare.core.vo.PermissionVO;
import com.zfc.eldercare.core.vo.SysResourceVO;

import java.util.List;

/**
 * 权限管理服务（RBAC 授权管理，文档 5.1）。
 */
public interface PermissionService {

    /** 权限分页（可按关键字筛选） */
    PageVO<PermissionVO> permissionPage(String keyword, int page, int size);

    /** 权限详情 */
    PermissionVO permissionDetail(Long id);

    /** 新增权限 */
    Long createPermission(PermissionDTO dto);

    /** 更新权限 */
    void updatePermission(Long id, PermissionDTO dto);

    /** 删除权限 */
    void deletePermission(Long id);

    /** 查权限已挂的资源列表 */
    List<SysResourceVO> permissionResources(Long permissionId);

    /** 全量重配权限资源（permission_resource） */
    void assignResources(Long permissionId, PermissionResourceDTO dto);
}
