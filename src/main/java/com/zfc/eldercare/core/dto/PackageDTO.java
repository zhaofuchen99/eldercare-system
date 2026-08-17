package com.zfc.eldercare.core.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 体检套餐新增/更新 DTO（体检预约模块，管理端）。
 * 字段均可选；新增时 name 必填（服务层校验），其余缺省按默认值。
 */
public record PackageDTO(

        @Size(max = 200, message = "套餐名称不能超过 200 字")
        String name,

        @Size(max = 500, message = "封面图 URL 不能超过 500 字")
        String coverUrl,

        @Size(max = 2000, message = "套餐描述不能超过 2000 字")
        String description,

        @Min(value = 0, message = "价格不能为负")
        @Max(value = 1_000_000, message = "价格超出范围")
        Integer price,

        @Size(max = 200, message = "适合人群不能超过 200 字")
        String suitablePeople,

        List<@Size(max = 100, message = "项目名称不能超过 100 字") String> items,

        @Pattern(regexp = "ENABLED|DISABLED", message = "状态只能是 ENABLED 或 DISABLED")
        String status
) {
}
