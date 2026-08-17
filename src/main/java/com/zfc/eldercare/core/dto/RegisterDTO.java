package com.zfc.eldercare.core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 注册请求（手机号 + 短信验证码 + 密码）。
 * 密码复杂度：长度 ≥ 8，且同时包含字母和数字（文档 5.1 密码安全）。
 */
public record RegisterDTO(
        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String phone,

        @NotBlank(message = "验证码不能为空")
        String code,

        @NotBlank(message = "密码不能为空")
        @Size(min = 8, message = "密码长度不能少于 8 位")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "密码必须同时包含字母和数字")
        String password) {
}
