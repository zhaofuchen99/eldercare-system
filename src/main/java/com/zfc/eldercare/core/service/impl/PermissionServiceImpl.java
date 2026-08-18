package com.zfc.eldercare.core.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.zfc.eldercare.core.dto.PermissionDTO;
import com.zfc.eldercare.core.dto.PermissionResourceDTO;
import com.zfc.eldercare.core.entity.Permission;
import com.zfc.eldercare.core.exception.BusinessException;
import com.zfc.eldercare.core.mapper.PermissionMapper;
import com.zfc.eldercare.core.mapper.PermissionResourceMapper;
import com.zfc.eldercare.core.mapper.SysResourceMapper;
import com.zfc.eldercare.core.service.PermissionService;
import com.zfc.eldercare.core.service.RbacService;
import com.zfc.eldercare.core.vo.PageVO;
import com.zfc.eldercare.core.vo.PermissionVO;
import com.zfc.eldercare.core.vo.SysResourceVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 权限管理服务实现（RBAC 授权管理，文档 5.1）。
 */
@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final PermissionMapper permissionMapper;
    private final PermissionResourceMapper permissionResourceMapper;
    private final SysResourceMapper sysResourceMapper;
    private final RbacService rbacService;

    @Override
    public PageVO<PermissionVO> permissionPage(String keyword, int page, int size) {
        PageHelper.startPage(page, size);
        List<Permission> list = permissionMapper.selectPage(keyword);
        PageInfo<Permission> pageInfo = new PageInfo<>(list);
        List<PermissionVO> voList = list.stream().map(PermissionVO::from).toList();
        return new PageVO<>(pageInfo.getPageNum(), pageInfo.getPageSize(),
                pageInfo.getTotal(), pageInfo.getPages(), voList);
    }

    @Override
    public PermissionVO permissionDetail(Long id) {
        return PermissionVO.from(requirePermission(id));
    }

    @Override
    @Transactional
    public Long createPermission(PermissionDTO dto) {
        if (permissionMapper.selectByCode(dto.permissionCode()) != null) {
            throw new BusinessException(409, "权限编码已存在");
        }
        Permission permission = new Permission();
        permission.setPermissionCode(dto.permissionCode());
        permission.setPermissionName(dto.permissionName());
        permission.setDescription(dto.description());
        permissionMapper.insert(permission);
        return permission.getId();
    }

    @Override
    @Transactional
    public void updatePermission(Long id, PermissionDTO dto) {
        requirePermission(id);
        Permission update = new Permission();
        update.setId(id);
        update.setPermissionCode(dto.permissionCode());
        update.setPermissionName(dto.permissionName());
        update.setDescription(dto.description());
        permissionMapper.update(update);
        rbacService.evictRbac();
    }

    @Override
    @Transactional
    public void deletePermission(Long id) {
        requirePermission(id);
        permissionMapper.delete(id);
        rbacService.evictRbac();
    }

    @Override
    public List<SysResourceVO> permissionResources(Long permissionId) {
        requirePermission(permissionId);
        List<Long> resourceIds = permissionResourceMapper.selectResourceIdsByPermissionId(permissionId);
        if (resourceIds.isEmpty()) {
            return List.of();
        }
        return sysResourceMapper.selectByIds(resourceIds).stream().map(SysResourceVO::from).toList();
    }

    @Override
    @Transactional
    public void assignResources(Long permissionId, PermissionResourceDTO dto) {
        requirePermission(permissionId);
        List<Long> resourceIds = dto.resourceIds().stream().distinct().toList();
        if (!resourceIds.isEmpty() && sysResourceMapper.selectByIds(resourceIds).size() != resourceIds.size()) {
            throw new BusinessException(404, "存在无效的资源");
        }
        permissionResourceMapper.deleteByPermissionId(permissionId);
        for (Long resourceId : resourceIds) {
            permissionResourceMapper.insert(permissionId, resourceId);
        }
        rbacService.evictRbac();
    }

    private Permission requirePermission(Long id) {
        Permission permission = permissionMapper.selectById(id);
        if (permission == null) {
            throw new BusinessException(404, "权限不存在");
        }
        return permission;
    }
}
