package com.zfc.eldercare.core.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 活动报名实体（对应 activity_registration 表，详细设计文档 6.3.12）。
 * check_in_status 编码：NOT_CHECKED_IN未签到/CHECKED_IN已签到
 */
@Data
public class ActivityRegistration {

    private Long id;

    private Long userId;

    private Long activityId;

    private String checkInStatus;

    /** 签到时间（未签到为 NULL） */
    private LocalDateTime checkInTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer deleted;
}
