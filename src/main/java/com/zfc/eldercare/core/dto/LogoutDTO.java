package com.zfc.eldercare.core.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 登出请求（携带 Refresh Token；Access Token 从请求头 Authorization 获取）。
 */
public record LogoutDTO(
        @NotBlank(message = "refreshToken 不能为空")
        String refreshToken) {
}
