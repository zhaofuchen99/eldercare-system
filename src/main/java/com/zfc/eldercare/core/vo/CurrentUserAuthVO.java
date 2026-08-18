package com.zfc.eldercare.core.vo;

import java.util.List;

/**
 * 当前登录用户的权限信息 VO（前端菜单/按钮显隐驱动，文档 8.2）。
 * roles：角色码列表；permissions：权限码列表；menus：菜单资源树；buttons：按钮资源列表。
 */
public record CurrentUserAuthVO(
        List<String> roles,
        List<String> permissions,
        List<SysResourceNodeVO> menus,
        List<SysResourceVO> buttons
) {
}
