package com.zfc.eldercare.core.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 重置会员密码 DTO（会员管理模块，管理端）。
 * password 可空：为空时由系统生成随机密码（含字母数字，≥8 位）；指定时校验复杂度。
 */
public record ResetPasswordDTO(

        @Size(min = 8, message = "密码长度不能少于 8 位")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "密码必须同时包含字母和数字")
        String password
) {
}
