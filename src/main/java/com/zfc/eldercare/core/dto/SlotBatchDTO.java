package com.zfc.eldercare.core.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * 批量生成预约时段 DTO（体检预约模块，管理端）。
 * dates × timeRanges 笛卡尔积生成时段；已存在的日期+时间段自动跳过。
 */
public record SlotBatchDTO(

        @NotNull(message = "请选择体检套餐")
        Long packageId,

        @NotEmpty(message = "请至少选择一天")
        List<@NotNull(message = "日期不能为空") LocalDate> dates,

        @NotEmpty(message = "请至少填写一个时间段")
        List<@Size(max = 50, message = "时间段格式不正确") String> timeRanges,

        @Min(value = 1, message = "最大预约人数至少为 1")
        @Max(value = 1000, message = "最大预约人数超出范围")
        Integer maxCount
) {
}
