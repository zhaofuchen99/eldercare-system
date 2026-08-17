package com.zfc.eldercare.core.vo;

import com.zfc.eldercare.core.entity.User;

/**
 * 登录/注册成功响应：双 Token + 用户信息。
 */
public record LoginVO(String accessToken, String refreshToken, String tokenType, long expiresIn, UserInfoVO userInfo) {

    public static LoginVO of(String accessToken, String refreshToken, long expiresInSeconds, User user) {
        return new LoginVO(accessToken, refreshToken, "Bearer", expiresInSeconds, UserInfoVO.from(user));
    }
}
