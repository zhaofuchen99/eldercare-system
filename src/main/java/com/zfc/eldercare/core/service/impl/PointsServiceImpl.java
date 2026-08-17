package com.zfc.eldercare.core.service.impl;

import com.zfc.eldercare.core.entity.PointTransaction;
import com.zfc.eldercare.core.entity.User;
import com.zfc.eldercare.core.mapper.PointTransactionMapper;
import com.zfc.eldercare.core.mapper.SysConfigMapper;
import com.zfc.eldercare.core.mapper.UserMapper;
import com.zfc.eldercare.core.service.PointsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 积分服务实现。
 */
@Service
@RequiredArgsConstructor
public class PointsServiceImpl implements PointsService {

    /** 注册赠送积分配置键 */
    private static final String KEY_REGISTER_BONUS = "register_bonus_points";
    /** 默认注册赠送积分（配置缺失时兜底） */
    private static final int DEFAULT_REGISTER_BONUS = 100;

    private final UserMapper userMapper;
    private final PointTransactionMapper pointTransactionMapper;
    private final SysConfigMapper sysConfigMapper;

    @Override
    @Transactional
    public void registerBonus(Long userId) {
        String value = sysConfigMapper.selectValueByKey(KEY_REGISTER_BONUS);
        int amount = value != null ? Integer.parseInt(value) : DEFAULT_REGISTER_BONUS;

        User user = userMapper.selectById(userId);
        if (user == null) {
            return;
        }

        // 原子增加积分（文档 9.3）
        userMapper.updatePoints(userId, amount);

        // 写获得流水：FIFO 批次，1 年有效（文档 5.8 / 6.3.18）
        PointTransaction tx = new PointTransaction();
        tx.setUserId(userId);
        tx.setType("REGISTER_BONUS");
        tx.setChangeAmount(amount);
        tx.setBalanceAfter(user.getPoints() + amount);
        tx.setRemainAmount(amount);
        tx.setExpireTime(LocalDateTime.now().plusYears(1));
        tx.setDescription("注册赠送积分");
        pointTransactionMapper.insert(tx);
    }
}
