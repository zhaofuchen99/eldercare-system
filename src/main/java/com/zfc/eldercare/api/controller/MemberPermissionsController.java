package com.zfc.eldercare.api.controller;

import com.zfc.eldercare.core.common.Result;
import com.zfc.eldercare.core.service.RbacService;
import com.zfc.eldercare.core.vo.CurrentUserAuthVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 当前登录用户权限信息接口（/api/member/permissions，文档 8.2 前端菜单/按钮显隐）。
 * MEMBER 与 ADMIN 均可访问（/api/member/** 已对 ADMIN 放行）。
 */
@RestController
@RequestMapping("/api/member/permissions")
@RequiredArgsConstructor
public class MemberPermissionsController {

    private final RbacService rbacService;

    /** 当前用户角色/权限/菜单树/按钮资源 */
    @GetMapping
    public Result<CurrentUserAuthVO> current(@AuthenticationPrincipal Long userId) {
        return Result.success(rbacService.currentUserAuth(userId));
    }
}
