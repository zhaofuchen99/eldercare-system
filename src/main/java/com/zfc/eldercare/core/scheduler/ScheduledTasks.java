package com.zfc.eldercare.core.scheduler;

import com.zfc.eldercare.core.service.PointsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 系统定时任务统一入口（详细设计文档 5.12）。
 * 单机项目使用 Spring 内置 @Scheduled + @EnableScheduling 即可，无需引入分布式任务框架；
 * 任务执行失败需记录告警日志，便于人工介入。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledTasks {

    private final PointsService pointsService;

    /**
     * 清理过期积分（每天凌晨 2 点，文档 5.12）：
     * 扫描 point_transaction 中已过期且未消费的获得批次，生成 EXPIRE 过期流水并原子扣减 user.points。
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void expirePoints() {
        try {
            int count = pointsService.expireExpiredPoints();
            if (count > 0) {
                log.info("积分过期清理完成，共清理 {} 个批次", count);
            }
        } catch (Exception e) {
            log.error("积分过期清理任务执行失败", e);
        }
    }
}
