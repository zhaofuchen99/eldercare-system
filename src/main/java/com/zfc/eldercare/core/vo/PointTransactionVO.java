package com.zfc.eldercare.core.vo;

import com.zfc.eldercare.core.entity.PointTransaction;

import java.time.LocalDateTime;

/**
 * 积分流水 VO（会员端积分明细，文档 5.8 / 6.3.18）。
 */
public record PointTransactionVO(
        Long id,
        String type,
        Integer changeAmount,
        Integer balanceAfter,
        String description,
        Long refId,
        LocalDateTime expireTime,
        LocalDateTime createTime
) {
    public static PointTransactionVO from(PointTransaction t) {
        return new PointTransactionVO(t.getId(), t.getType(), t.getChangeAmount(),
                t.getBalanceAfter(), t.getDescription(), t.getRefId(), t.getExpireTime(), t.getCreateTime());
    }
}
