package com.zfc.eldercare.core.vo;

import com.zfc.eldercare.core.entity.User;

/**
 * 用户信息响应（不含密码）。
 */
public record UserInfoVO(Long id, String phone, String realName, String avatar,
                         String memberLevel, Integer points, String role) {

    public static UserInfoVO from(User user) {
        return new UserInfoVO(user.getId(), user.getPhone(), user.getRealName(),
                user.getAvatar(), user.getMemberLevel(), user.getPoints(), user.getRole());
    }
}
