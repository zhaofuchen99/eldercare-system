package com.zfc.eldercare.core.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 提交体检预约 DTO（体检预约模块）。
 */
public record AppointmentCreateDTO(

        @NotNull(message = "请选择预约时段")
        Long slotId
) {
}
