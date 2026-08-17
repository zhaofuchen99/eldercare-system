package com.zfc.eldercare.core.entity;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 预约时段实体（对应 appointment_slot 表，详细设计文档 6.3.9）。
 * status 编码：AVAILABLE可预约/FULL已满/CLOSED已关闭
 */
@Data
public class AppointmentSlot {

    private Long id;

    /** 套餐 ID */
    private Long packageId;

    /** 预约日期 */
    private LocalDate appointDate;

    /** 时间段，如 "09:00-10:00" */
    private String timeRange;

    /** 最大预约人数 */
    private Integer maxCount;

    /** 当前已预约人数 */
    private Integer currentCount;

    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer deleted;
}
