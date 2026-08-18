package com.zfc.eldercare.api.controller;

import com.zfc.eldercare.core.common.Result;
import com.zfc.eldercare.core.dto.RoleDTO;
import com.zfc.eldercare.core.dto.RolePermissionDTO;
import com.zfc.eldercare.core.dto.RoleUsersDTO;
import com.zfc.eldercare.core.service.RoleService;
import com.zfc.eldercare.core.vo.PageVO;
import com.zfc.eldercare.core.vo.PermissionVO;
import com.zfc.eldercare.core.vo.RoleUserVO;
import com.zfc.eldercare.core.vo.RoleVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端角色管理接口（/api/admin/role，RBAC 授权管理，文档 5.1）。
 * 角色增删改查、角色授权限、角色分配用户。
 */
@RestController
@RequestMapping("/api/admin/role")
@RequiredArgsConstructor
public class AdminRoleController {

    private final RoleService roleService;

    /** 角色分页 */
    @PreAuthorize("hasAuthority('admin:role:manage')")
    @GetMapping("/page")
    public Result<PageVO<RoleVO>> page(@RequestParam(required = false) String keyword,
                                       @RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "10") int size) {
        return Result.success(roleService.rolePage(keyword, page, size));
    }

    /** 角色详情 */
    @PreAuthorize("hasAuthority('admin:role:manage')")
    @GetMapping("/{id}")
    public Result<RoleVO> detail(@PathVariable Long id) {
        return Result.success(roleService.roleDetail(id));
    }

    /** 查角色已分配的权限列表 */
    @PreAuthorize("hasAuthority('admin:role:manage')")
    @GetMapping("/{id}/permissions")
    public Result<List<PermissionVO>> permissions(@PathVariable Long id) {
        return Result.success(roleService.rolePermissions(id));
    }

    /** 全量重配角色权限 */
    @PreAuthorize("hasAuthority('admin:role:manage')")
    @PutMapping("/{id}/permissions")
    public Result<Void> assignPermissions(@PathVariable Long id, @Valid @RequestBody RolePermissionDTO dto) {
        roleService.assignPermissions(id, dto);
        return Result.success("权限分配成功", null);
    }

    /** 查该角色下的用户列表 */
    @PreAuthorize("hasAuthority('admin:role:manage')")
    @GetMapping("/{id}/users")
    public Result<PageVO<RoleUserVO>> users(@PathVariable Long id,
                                            @RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size) {
        return Result.success(roleService.roleUsers(id, page, size));
    }

    /** 把角色授予指定用户（替换其原角色） */
    @PreAuthorize("hasAuthority('admin:role:manage')")
    @PutMapping("/{id}/users")
    public Result<Void> assignUsers(@PathVariable Long id, @Valid @RequestBody RoleUsersDTO dto) {
        roleService.assignUsers(id, dto);
        return Result.success("用户分配成功", null);
    }

    /** 新增角色 */
    @PreAuthorize("hasAuthority('admin:role:manage')")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody RoleDTO dto) {
        return Result.success("创建成功", roleService.createRole(dto));
    }

    /** 更新角色 */
    @PreAuthorize("hasAuthority('admin:role:manage')")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody RoleDTO dto) {
        roleService.updateRole(id, dto);
        return Result.success("更新成功", null);
    }

    /** 删除角色 */
    @PreAuthorize("hasAuthority('admin:role:manage')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        roleService.deleteRole(id);
        return Result.success("删除成功", null);
    }
}
