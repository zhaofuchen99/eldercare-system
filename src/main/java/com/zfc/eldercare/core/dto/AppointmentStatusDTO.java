package com.zfc.eldercare.core.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 管理端预约状态变更 DTO（体检预约模块）。
 * 支持 CONFIRMED（待确认→已确认）、CANCELED（取消并退还积分/名额）。
 */
public record AppointmentStatusDTO(

        @NotBlank(message = "状态不能为空")
        String status
) {
}
