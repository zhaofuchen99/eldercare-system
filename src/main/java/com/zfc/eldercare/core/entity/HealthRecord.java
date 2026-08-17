package com.zfc.eldercare.core.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 健康记录实体（对应 health_record 表，详细设计文档 6.3.4）。
 */
@Data
public class HealthRecord {

    private Long id;

    private Long userId;

    /** 收缩压（mmHg） */
    private Integer systolic;

    /** 舒张压（mmHg） */
    private Integer diastolic;

    /** 血糖（mmol/L） */
    private BigDecimal bloodSugar;

    /** 心率（次/分） */
    private Integer heartRate;

    /** 体重（kg） */
    private BigDecimal weight;

    /** BMI 指数（weight / (height/100)^2，身高取 user 表） */
    private BigDecimal bmi;

    /** 备注 */
    private String memo;

    /** 记录时间 */
    private LocalDateTime recordedTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer deleted;
}
