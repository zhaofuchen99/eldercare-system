package com.zfc.eldercare.api.controller;

import com.zfc.eldercare.core.common.Result;
import com.zfc.eldercare.core.dto.PermissionDTO;
import com.zfc.eldercare.core.dto.PermissionResourceDTO;
import com.zfc.eldercare.core.service.PermissionService;
import com.zfc.eldercare.core.vo.PageVO;
import com.zfc.eldercare.core.vo.PermissionVO;
import com.zfc.eldercare.core.vo.SysResourceVO;
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
 * 管理端权限管理接口（/api/admin/permission，RBAC 授权管理，文档 5.1）。
 * 权限增删改查、权限挂资源。
 */
@RestController
@RequestMapping("/api/admin/permission")
@RequiredArgsConstructor
public class AdminPermissionController {

    private final PermissionService permissionService;

    /** 权限分页 */
    @PreAuthorize("hasAuthority('admin:permission:manage')")
    @GetMapping("/page")
    public Result<PageVO<PermissionVO>> page(@RequestParam(required = false) String keyword,
                                             @RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "10") int size) {
        return Result.success(permissionService.permissionPage(keyword, page, size));
    }

    /** 权限详情 */
    @PreAuthorize("hasAuthority('admin:permission:manage')")
    @GetMapping("/{id}")
    public Result<PermissionVO> detail(@PathVariable Long id) {
        return Result.success(permissionService.permissionDetail(id));
    }

    /** 查权限已挂的资源列表 */
    @PreAuthorize("hasAuthority('admin:permission:manage')")
    @GetMapping("/{id}/resources")
    public Result<List<SysResourceVO>> resources(@PathVariable Long id) {
        return Result.success(permissionService.permissionResources(id));
    }

    /** 全量重配权限资源 */
    @PreAuthorize("hasAuthority('admin:permission:manage')")
    @PutMapping("/{id}/resources")
    public Result<Void> assignResources(@PathVariable Long id, @Valid @RequestBody PermissionResourceDTO dto) {
        permissionService.assignResources(id, dto);
        return Result.success("资源分配成功", null);
    }

    /** 新增权限 */
    @PreAuthorize("hasAuthority('admin:permission:manage')")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody PermissionDTO dto) {
        return Result.success("创建成功", permissionService.createPermission(dto));
    }

    /** 更新权限 */
    @PreAuthorize("hasAuthority('admin:permission:manage')")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody PermissionDTO dto) {
        permissionService.updatePermission(id, dto);
        return Result.success("更新成功", null);
    }

    /** 删除权限 */
    @PreAuthorize("hasAuthority('admin:permission:manage')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        permissionService.deletePermission(id);
        return Result.success("删除成功", null);
    }
}
