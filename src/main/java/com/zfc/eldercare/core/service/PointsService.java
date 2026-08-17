package com.zfc.eldercare.core.service;

import com.zfc.eldercare.core.vo.PageVO;
import com.zfc.eldercare.core.vo.PointTransactionVO;

/**
 * 积分服务（文档 5.8 / 6.3.18）。
 * 获得类：注册/评测完成；消费类：体检预约 FIFO 扣减与取消退还。
 */
public interface PointsService {

    /** 注册赠送积分：从 sys_config 读注册赠送值，原子增加 user.points 并写获得流水（1 年有效） */
    void registerBonus(Long userId);

    /** 完成健康评测赠送积分（默认 20，sys_config 可配） */
    void assessmentBonus(Long userId);

    /** 活动签到赠送积分（默认 50，sys_config checkin_bonus_points 可配） */
    void checkinBonus(Long userId);

    /**
     * 体检预约消费积分：按 FIFO 扣减最早获得且未过期的积分批次，每个被扣批次生成一条
     * APPOINTMENT_CONSUME 消费流水（batch_tx_id 指向批次）。调用方须在同一事务内先完成
     * user.points 的原子扣减（UserMapper.deductPoints）再调用本方法。
     */
    void consumeAppointment(Long userId, Long refId, int amount);

    /**
     * 预约取消退还积分：对应消费流水逻辑删除（deleted=1）、批次 remain_amount 加回
     * （原有效期不重新起算），并同步原子加回 user.points。返回实际退还积分数。
     */
    int refundAppointment(Long userId, Long refId);

    /** 积分明细分页（会员端，按时间倒序：获得/消费/过期等流水） */
    PageVO<PointTransactionVO> pointPage(Long userId, int page, int size);
}
