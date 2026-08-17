package com.zfc.eldercare.core.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站内消息实体（对应 message 表，详细设计文档 6.3.16）。
 * type 编码：APPOINTMENT预约/ACTIVITY活动/SYSTEM系统/HEALTH_REMINDER健康提醒
 */
@Data
public class Message {

    private Long id;

    private Long userId;

    private String title;

    private String content;

    private String type;

    /** 是否已读：0 未读/1 已读 */
    private Integer isRead;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer deleted;
}
