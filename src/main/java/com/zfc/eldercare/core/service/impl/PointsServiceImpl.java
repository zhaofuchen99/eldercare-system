package com.zfc.eldercare.core.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.zfc.eldercare.core.entity.PointTransaction;
import com.zfc.eldercare.core.entity.User;
import com.zfc.eldercare.core.mapper.PointTransactionMapper;
import com.zfc.eldercare.core.mapper.SysConfigMapper;
import com.zfc.eldercare.core.mapper.UserMapper;
import com.zfc.eldercare.core.service.PointsService;
import com.zfc.eldercare.core.vo.PageVO;
import com.zfc.eldercare.core.vo.PointTransactionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 积分服务实现（文档 5.8 / 6.3.18）。
 */
@Service
@RequiredArgsConstructor
public class PointsServiceImpl implements PointsService {

    /** 注册赠送积分配置键 */
    private static final String KEY_REGISTER_BONUS = "register_bonus_points";
    /** 默认注册赠送积分（配置缺失时兜底） */
    private static final int DEFAULT_REGISTER_BONUS = 100;
    /** 完成评测赠送积分配置键 */
    private static final String KEY_ASSESSMENT_BONUS = "health_assessment_bonus_points";
    /** 默认评测赠送积分（配置缺失时兜底） */
    private static final int DEFAULT_ASSESSMENT_BONUS = 20;
    /** 活动签到赠送积分配置键 */
    private static final String KEY_CHECKIN_BONUS = "checkin_bonus_points";
    /** 默认签到赠送积分（配置缺失时兜底） */
    private static final int DEFAULT_CHECKIN_BONUS = 50;
    /** 体检预约消费流水类型与说明 */
    private static final String TYPE_APPOINTMENT_CONSUME = "APPOINTMENT_CONSUME";
    private static final String DESC_APPOINTMENT_CONSUME = "体检预约扣除积分";

    private final UserMapper userMapper;
    private final PointTransactionMapper pointTransactionMapper;
    private final SysConfigMapper sysConfigMapper;

    @Override
    @Transactional
    public void registerBonus(Long userId) {
        grant(userId, readConfig(KEY_REGISTER_BONUS, DEFAULT_REGISTER_BONUS),
                "REGISTER_BONUS", "注册赠送积分");
    }

    @Override
    @Transactional
    public void assessmentBonus(Long userId) {
        grant(userId, readConfig(KEY_ASSESSMENT_BONUS, DEFAULT_ASSESSMENT_BONUS),
                "ASSESSMENT_COMPLETE", "完成健康评测赠送积分");
    }

    @Override
    @Transactional
    public void checkinBonus(Long userId) {
        grant(userId, readConfig(KEY_CHECKIN_BONUS, DEFAULT_CHECKIN_BONUS),
                "ACTIVITY_CHECKIN", "活动签到赠送积分");
    }

    @Override
    @Transactional
    public void consumeAppointment(Long userId, Long refId, int amount) {
        if (amount <= 0) {
            return;
        }
        // 取可用获得批次（FIFO：未过期、剩余>0），行锁防并发双扣
        List<PointTransaction> batches = pointTransactionMapper.selectConsumableBatches(userId);
        int remaining = amount;
        int balance = userMapper.selectById(userId).getPoints();
        for (PointTransaction batch : batches) {
            if (remaining <= 0) {
                break;
            }
            int use = Math.min(remaining, batch.getRemainAmount());
            pointTransactionMapper.decreaseRemain(batch.getId(), use);
            PointTransaction tx = new PointTransaction();
            tx.setUserId(userId);
            tx.setType(TYPE_APPOINTMENT_CONSUME);
            tx.setChangeAmount(-use);
            tx.setBalanceAfter(balance);
            tx.setRemainAmount(0);
            tx.setBatchTxId(batch.getId());
            tx.setDescription(DESC_APPOINTMENT_CONSUME);
            tx.setRefId(refId);
            pointTransactionMapper.insert(tx);
            balance -= use;
            remaining -= use;
        }
    }

    @Override
    @Transactional
    public int refundAppointment(Long userId, Long refId) {
        List<PointTransaction> consumes = pointTransactionMapper.selectConsumeByRef(refId);
        if (consumes.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (PointTransaction consume : consumes) {
            int refund = -consume.getChangeAmount();
            pointTransactionMapper.increaseRemain(consume.getBatchTxId(), refund);
            total += refund;
        }
        pointTransactionMapper.softDeleteByRef(refId);
        userMapper.updatePoints(userId, total);
        return total;
    }

    @Override
    public PageVO<PointTransactionVO> pointPage(Long userId, int page, int size) {
        PageHelper.startPage(page, size);
        List<PointTransaction> list = pointTransactionMapper.selectPageByUserId(userId);
        PageInfo<PointTransaction> pageInfo = new PageInfo<>(list);
        List<PointTransactionVO> voList = list.stream().map(PointTransactionVO::from).toList();
        return new PageVO<>(pageInfo.getPageNum(), pageInfo.getPageSize(),
                pageInfo.getTotal(), pageInfo.getPages(), voList);
    }

    /** 读 sys_config，缺失时用默认值 */
    private int readConfig(String key, int defaultVal) {
        String value = sysConfigMapper.selectValueByKey(key);
        return value != null ? Integer.parseInt(value) : defaultVal;
    }

    /**
     * 通用获得积分：原子增加 user.points，写获得类流水（FIFO 批次，1 年有效，文档 5.8 / 6.3.18）。
     */
    private void grant(Long userId, int amount, String type, String description) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return;
        }
        userMapper.updatePoints(userId, amount);

        PointTransaction tx = new PointTransaction();
        tx.setUserId(userId);
        tx.setType(type);
        tx.setChangeAmount(amount);
        tx.setBalanceAfter(user.getPoints() + amount);
        tx.setRemainAmount(amount);
        tx.setExpireTime(LocalDateTime.now().plusYears(1));
        tx.setDescription(description);
        pointTransactionMapper.insert(tx);
    }
}
