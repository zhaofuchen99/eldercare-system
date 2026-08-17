package com.zfc.eldercare.core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 会员等级调整 DTO（会员管理模块，管理端）。
 * 等级编码：NORMAL/SILVER/GOLD/PLATINUM/DIAMOND（与 MemberLevel 枚举一致）。
 */
public record MemberLevelDTO(

        @NotBlank(message = "会员等级不能为空")
        @Pattern(regexp = "NORMAL|SILVER|GOLD|PLATINUM|DIAMOND", message = "会员等级只能是 NORMAL/SILVER/GOLD/PLATINUM/DIAMOND")
        String memberLevel
) {
}
