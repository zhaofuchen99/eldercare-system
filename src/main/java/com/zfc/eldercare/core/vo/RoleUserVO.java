package com.zfc.eldercare.core.vo;

import com.zfc.eldercare.core.entity.User;

import java.time.LocalDateTime;

/**
 * 角色下用户 VO（管理端「角色下的用户」列表）。
 */
public record RoleUserVO(
        Long id,
        String phone,
        String realName,
        String role,
        LocalDateTime createTime
) {
    public static RoleUserVO from(User u) {
        return new RoleUserVO(u.getId(), u.getPhone(), u.getRealName(), u.getRole(), u.getCreateTime());
    }
}
