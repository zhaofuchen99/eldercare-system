package com.zfc.eldercare.core.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 系统配置更新 DTO（管理端，文档 5.11）。
 */
public record SysConfigUpdateDTO(

        @NotBlank(message = "配置值不能为空")
        String configValue
) {
}
