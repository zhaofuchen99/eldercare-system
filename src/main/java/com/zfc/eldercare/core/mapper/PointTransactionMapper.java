package com.zfc.eldercare.core.mapper;

import com.zfc.eldercare.core.entity.PointTransaction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 积分流水表 Mapper（文档 6.3.18）。
 */
@Mapper
public interface PointTransactionMapper {

    int insert(PointTransaction transaction);

    /** 可用获得批次（FIFO：未过期、剩余 > 0，按过期时间升序），FOR UPDATE 锁定防并发双扣 */
    List<PointTransaction> selectConsumableBatches(@Param("userId") Long userId);

    /** 会员端：我的积分流水分页（按时间倒序，文档 5.8 积分明细） */
    List<PointTransaction> selectPageByUserId(@Param("userId") Long userId);

    /** 已过期且未消费的获得批次（remain_amount > 0 且 expire_time < NOW()），FOR UPDATE 锁定（每日过期清理任务） */
    List<PointTransaction> selectExpiredBatches();

    /** 清理过期批次剩余积分（remain_amount 置 0） */
    int clearRemain(@Param("id") Long id);

    /** 预约消费流水（取消退还时还原批次用） */
    List<PointTransaction> selectConsumeByRef(@Param("refId") Long refId);

    /** 扣减批次剩余积分（FIFO 消费，remain_amount >= delta 才成功） */
    int decreaseRemain(@Param("id") Long id, @Param("delta") int delta);

    /** 加回批次剩余积分（取消退还，原有效期不重新起算） */
    int increaseRemain(@Param("id") Long id, @Param("delta") int delta);

    /** 消费流水逻辑删除（取消退还） */
    int softDeleteByRef(@Param("refId") Long refId);
}
