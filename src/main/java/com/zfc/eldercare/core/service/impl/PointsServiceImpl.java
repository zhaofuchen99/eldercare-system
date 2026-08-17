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
