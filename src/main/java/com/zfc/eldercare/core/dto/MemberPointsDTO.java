package com.zfc.eldercare.core.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 积分手动调整 DTO（会员管理模块，管理端）。
 * delta 为正=调增，为负=调减，为 0 无操作。
 */
public record MemberPointsDTO(

        @NotNull(message = "积分调整值不能为空")
        Integer delta
) {
}
