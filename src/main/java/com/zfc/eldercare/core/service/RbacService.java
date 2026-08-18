package com.zfc.eldercare.core.service;

import com.zfc.eldercare.core.entity.SysResource;
import com.zfc.eldercare.core.vo.CurrentUserAuthVO;

import java.util.List;
import java.util.Set;

/**
 * RBAC 权限解析服务（详细设计文档 5.1 / 8.2）。
 * 用户→角色→权限→资源四层解析，映射缓存至 Redis，授权变更时 evictRbac 刷新。
 */
public interface RbacService {

    /** 用户拥有的角色码集合（user_role→role，空时由调用方回退 JWT claim role） */
    Set<String> rolesOf(Long userId);

    /** 用户拥有的权限码集合（role→permission） */
    Set<String> permissionsOf(Long userId);

    /** 用户可访问的资源（permission→resource，含 API/MENU/BUTTON） */
    List<SysResource> resourcesOf(Long userId);

    /** 当前登录用户的权限信息（角色/权限/菜单树/按钮，供前端渲染） */
    CurrentUserAuthVO currentUserAuth(Long userId);

    /** 清空全部 RBAC 缓存（角色/权限/资源/授权任何变更后调用） */
    void evictRbac();
}
