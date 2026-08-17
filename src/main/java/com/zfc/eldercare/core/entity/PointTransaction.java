package com.zfc.eldercare.core.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 积分流水实体（对应 point_transaction 表，文档 6.3.18）。
 */
@Data
public class PointTransaction {

    private Long id;

    private Long userId;

    /** 类型：REGISTER_BONUS/ACTIVITY_CHECKIN/ASSESSMENT_COMPLETE/ADMIN_ADJUST/APPOINTMENT_CONSUME/EXPIRE */
    private String type;

    /** 变动积分（正=获得，负=扣减） */
    private Integer changeAmount;

    /** 变动后积分余额 */
    private Integer balanceAfter;

    /** 获得类流水的剩余可用积分（FIFO 消费） */
    private Integer remainAmount;

    /** 获得类流水过期时间（获得时间 + 1 年） */
    private LocalDateTime expireTime;

    /** 消耗类流水关联的被扣获得批次流水 ID */
    private Long batchTxId;

    private String description;

    /** 关联业务记录 ID（预约/报名/评测等） */
    private Long refId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer deleted;
}
