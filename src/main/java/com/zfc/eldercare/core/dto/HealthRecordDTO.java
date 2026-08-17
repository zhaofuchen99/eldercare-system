package com.zfc.eldercare.core.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.Range;

import java.math.BigDecimal;

/**
 * 健康数据录入 DTO（详细设计文档 5.2 / 7.2）。
 * 各项指标均可选，至少填写一项（校验在服务层判断）。
 */
public record HealthRecordDTO(

        @Range(min = 30, max = 250, message = "收缩压超出合理范围")
        Integer systolic,

        @Range(min = 30, max = 150, message = "舒张压超出合理范围")
        Integer diastolic,

        @DecimalMin(value = "1.0", message = "血糖超出合理范围")
        @DecimalMax(value = "30.0", message = "血糖超出合理范围")
        BigDecimal bloodSugar,

        @Range(min = 20, max = 250, message = "心率超出合理范围")
        Integer heartRate,

        @DecimalMin(value = "10.0", message = "体重超出合理范围")
        @DecimalMax(value = "300.0", message = "体重超出合理范围")
        BigDecimal weight,

        @Size(max = 500, message = "备注不能超过 500 字")
        String memo
) {
}
