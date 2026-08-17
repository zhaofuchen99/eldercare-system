package com.zfc.eldercare.core.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 健康指导实体（对应 health_guidance 表，详细设计文档 6.3.13）。
 * type 编码：DIET饮食/EXERCISE运动/DAILY作息/DATA_SUMMARY数据小结
 * indicator 编码：SYSTOLIC/DIASTOLIC/BLOOD_SUGAR/HEART_RATE/BMI/WEIGHT（用于同日去重与 AI 建议生成）
 */
@Data
public class HealthGuidance {

    private Long id;

    private Long userId;

    private String type;

    private String indicator;

    private String content;

    /** 是否已读：0 未读/1 已读 */
    private Integer isRead;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer deleted;
}
