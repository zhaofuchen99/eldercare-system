package com.zfc.eldercare.core.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.zfc.eldercare.core.dto.RoleDTO;
import com.zfc.eldercare.core.dto.RolePermissionDTO;
import com.zfc.eldercare.core.dto.RoleUsersDTO;
import com.zfc.eldercare.core.entity.Role;
import com.zfc.eldercare.core.entity.User;
import com.zfc.eldercare.core.exception.BusinessException;
import com.zfc.eldercare.core.mapper.PermissionMapper;
import com.zfc.eldercare.core.mapper.RoleMapper;
import com.zfc.eldercare.core.mapper.RolePermissionMapper;
import com.zfc.eldercare.core.mapper.UserMapper;
import com.zfc.eldercare.core.mapper.UserRoleMapper;
import com.zfc.eldercare.core.service.RbacService;
import com.zfc.eldercare.core.service.RoleService;
import com.zfc.eldercare.core.vo.PageVO;
import com.zfc.eldercare.core.vo.PermissionVO;
import com.zfc.eldercare.core.vo.RoleUserVO;
import com.zfc.eldercare.core.vo.RoleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 角色管理服务实现（RBAC 授权管理，文档 5.1）。
 */
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private static final String BUILTIN_ADMIN = "ADMIN";
    private static final String BUILTIN_MEMBER = "MEMBER";

    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final UserRoleMapper userRoleMapper;
    private final UserMapper userMapper;
    private final RbacService rbacService;

    @Override
    public PageVO<RoleVO> rolePage(String keyword, int page, int size) {
        PageHelper.startPage(page, size);
        List<Role> list = roleMapper.selectPage(keyword);
        PageInfo<Role> pageInfo = new PageInfo<>(list);
        List<RoleVO> voList = list.stream().map(RoleVO::from).toList();
        return new PageVO<>(pageInfo.getPageNum(), pageInfo.getPageSize(),
                pageInfo.getTotal(), pageInfo.getPages(), voList);
    }

    @Override
    public RoleVO roleDetail(Long id) {
        return RoleVO.from(requireRole(id));
    }

    @Override
    @Transactional
    public Long createRole(RoleDTO dto) {
        if (roleMapper.selectByCode(dto.roleCode()) != null) {
            throw new BusinessException(409, "角色编码已存在");
        }
        Role role = new Role();
        role.setRoleCode(dto.roleCode());
        role.setRoleName(dto.roleName());
        role.setDescription(dto.description());
        role.setStatus(dto.status() == null ? 1 : dto.status());
        roleMapper.insert(role);
        return role.getId();
    }

    @Override
    @Transactional
    public void updateRole(Long id, RoleDTO dto) {
        Role role = requireRole(id);
        if (isBuiltin(role) && !role.getRoleCode().equals(dto.roleCode())) {
            throw new BusinessException("内置角色编码不允许修改");
        }
        Role update = new Role();
        update.setId(id);
        update.setRoleCode(dto.roleCode());
        update.setRoleName(dto.roleName());
        update.setDescription(dto.description());
        update.setStatus(dto.status());
        roleMapper.update(update);
        rbacService.evictRbac();
    }

    @Override
    @Transactional
    public void deleteRole(Long id) {
        Role role = requireRole(id);
        if (isBuiltin(role)) {
            throw new BusinessException("内置角色不允许删除");
        }
        roleMapper.delete(id);
        rbacService.evictRbac();
    }

    @Override
    public List<PermissionVO> rolePermissions(Long roleId) {
        requireRole(roleId);
        List<Long> permissionIds = rolePermissionMapper.selectPermissionIdsByRoleId(roleId);
        if (permissionIds.isEmpty()) {
            return List.of();
        }
        return permissionMapper.selectByIds(permissionIds).stream().map(PermissionVO::from).toList();
    }

    @Override
    @Transactional
    public void assignPermissions(Long roleId, RolePermissionDTO dto) {
        requireRole(roleId);
        List<Long> permissionIds = dto.permissionIds().stream().distinct().toList();
        if (!permissionIds.isEmpty() && permissionMapper.selectByIds(permissionIds).size() != permissionIds.size()) {
            throw new BusinessException(404, "存在无效的权限");
        }
        rolePermissionMapper.deleteByRoleId(roleId);
        for (Long permissionId : permissionIds) {
            rolePermissionMapper.insert(roleId, permissionId);
        }
        rbacService.evictRbac();
    }

    @Override
    public PageVO<RoleUserVO> roleUsers(Long roleId, int page, int size) {
        requireRole(roleId);
        List<Long> userIds = userRoleMapper.selectUserIdsByRoleId(roleId);
        int total = userIds.size();
        int pages = (total + size - 1) / size;
        int from = Math.min(Math.max((page - 1) * size, 0), total);
        int to = Math.min(page * size, total);
        List<Long> pageIds = userIds.subList(from, to);
        List<RoleUserVO> voList = pageIds.isEmpty() ? List.of()
                : userMapper.selectByIds(pageIds).stream().map(RoleUserVO::from).toList();
        return new PageVO<>(page, size, total, pages, voList);
    }

    @Override
    @Transactional
    public void assignUsers(Long roleId, RoleUsersDTO dto) {
        Role role = requireRole(roleId);
        List<Long> userIds = dto.userIds().stream().distinct().toList();
        List<User> users = userMapper.selectByIds(userIds);
        if (users.size() != userIds.size()) {
            throw new BusinessException(404, "存在无效的用户");
        }
        // 单角色模型：给这些用户授予该角色（替换其原角色），并双写 user.role 冗余字段保持一致
        for (User user : users) {
            userRoleMapper.deleteByUserId(user.getId());
            userRoleMapper.insert(user.getId(), role.getId());
            userMapper.updateRole(user.getId(), role.getRoleCode());
        }
        rbacService.evictRbac();
    }

    private Role requireRole(Long id) {
        Role role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(404, "角色不存在");
        }
        return role;
    }

    private boolean isBuiltin(Role role) {
        return BUILTIN_ADMIN.equals(role.getRoleCode()) || BUILTIN_MEMBER.equals(role.getRoleCode());
    }
}
