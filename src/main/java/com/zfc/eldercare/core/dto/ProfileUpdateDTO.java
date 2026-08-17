package com.zfc.eldercare.core.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 个人信息更新 DTO（会员中心，需求 4.1.8）。
 * 字段均可选，仅更新传入的字段（部分更新）。
 */
public record ProfileUpdateDTO(

        @Size(max = 50, message = "真实姓名不能超过 50 字")
        String realName,

        @Size(max = 20, message = "性别不能超过 20 字")
        String gender,

        @Past(message = "出生日期不能晚于今天")
        LocalDate birthDate,

        @DecimalMin(value = "50.0", message = "身高不能小于 50")
        @DecimalMax(value = "250.0", message = "身高不能大于 250")
        BigDecimal height,

        @Size(max = 500, message = "头像 URL 不能超过 500 字")
        String avatar,

        @Size(max = 20, message = "紧急联系人电话不能超过 20 字")
        String emergencyContact
) {
}
