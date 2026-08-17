package com.zfc.eldercare.core.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 刷新 Token 请求。
 */
public record RefreshTokenDTO(
        @NotBlank(message = "refreshToken 不能为空")
        String refreshToken) {
}
