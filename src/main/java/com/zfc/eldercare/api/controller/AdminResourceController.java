package com.zfc.eldercare.api.controller;

import com.zfc.eldercare.core.common.Result;
import com.zfc.eldercare.core.dto.SysResourceDTO;
import com.zfc.eldercare.core.service.SysResourceService;
import com.zfc.eldercare.core.vo.PageVO;
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

/**
 * 管理端资源管理接口（/api/admin/resource，RBAC 授权管理，文档 5.1）。
 * 接口/菜单/按钮三类资源增删改查。
 */
@RestController
@RequestMapping("/api/admin/resource")
@RequiredArgsConstructor
public class AdminResourceController {

    private final SysResourceService sysResourceService;

    /** 资源分页（可按类型/关键字筛选） */
    @PreAuthorize("hasAuthority('admin:resource:manage')")
    @GetMapping("/page")
    public Result<PageVO<SysResourceVO>> page(@RequestParam(required = false) String type,
                                              @RequestParam(required = false) String keyword,
                                              @RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "10") int size) {
        return Result.success(sysResourceService.resourcePage(type, keyword, page, size));
    }

    /** 资源详情 */
    @PreAuthorize("hasAuthority('admin:resource:manage')")
    @GetMapping("/{id}")
    public Result<SysResourceVO> detail(@PathVariable Long id) {
        return Result.success(sysResourceService.resourceDetail(id));
    }

    /** 新增资源 */
    @PreAuthorize("hasAuthority('admin:resource:manage')")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody SysResourceDTO dto) {
        return Result.success("创建成功", sysResourceService.createResource(dto));
    }

    /** 更新资源 */
    @PreAuthorize("hasAuthority('admin:resource:manage')")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody SysResourceDTO dto) {
        sysResourceService.updateResource(id, dto);
        return Result.success("更新成功", null);
    }

    /** 删除资源 */
    @PreAuthorize("hasAuthority('admin:resource:manage')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        sysResourceService.deleteResource(id);
        return Result.success("删除成功", null);
    }
}
