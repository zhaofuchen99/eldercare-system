package com.zfc.eldercare.core.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 登录请求（手机号 + 密码）。
 */
public record LoginDTO(
        @NotBlank(message = "手机号不能为空")
        String phone,

        @NotBlank(message = "密码不能为空")
        String password) {
}
