package com.zfc.eldercare.core.scheduler;

import com.zfc.eldercare.core.service.ActivityService;
import com.zfc.eldercare.core.service.AiChatService;
import com.zfc.eldercare.core.service.AppointmentService;
import com.zfc.eldercare.core.service.PointsService;
import com.zfc.eldercare.core.service.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 系统定时任务统一入口（详细设计文档 5.12）。
 * 单机项目使用 Spring 内置 @Scheduled + @EnableScheduling 即可，无需引入分布式任务框架；
 * 任务执行失败需记录告警日志，便于人工介入。
 *
 * 注：5.12 中的「同步健康数据每日统计」无对应统计表/统计口径可落地（为数据量增长后的可选扩展，
 * 见 5.9 实现说明），暂不实现。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledTasks {

    private final PointsService pointsService;
    private final AiChatService aiChatService;
    private final AppointmentService appointmentService;
    private final ActivityService activityService;
    private final SmsService smsService;

    /** 清理过期积分（每天凌晨 2 点）：已过期未消费批次生成 EXPIRE 流水并扣减余额 */
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

    /** 清理过期 AI 对话消息（每天凌晨 3 点，保留 6 个月，文档 5.12） */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanAiMessages() {
        try {
            aiChatService.cleanExpiredMessages();
        } catch (Exception e) {
            log.error("AI 对话消息清理任务执行失败", e);
        }
    }

    /** 归档过期历史预约（每天凌晨 4 点，保留 2 年，文档 5.12） */
    @Scheduled(cron = "0 0 4 * * ?")
    public void archiveAppointments() {
        try {
            appointmentService.archiveExpired();
        } catch (Exception e) {
            log.error("历史预约归档任务执行失败", e);
        }
    }

    /** 预约提醒推送（每天上午 9 点，次日 CONFIRMED 预约，文档 5.12） */
    @Scheduled(cron = "0 0 9 * * ?")
    public void sendAppointmentReminders() {
        try {
            appointmentService.sendTomorrowReminders();
        } catch (Exception e) {
            log.error("预约提醒推送任务执行失败", e);
        }
    }

    /** 活动提醒推送（每天上午 9 点，次日开始的活动，文档 5.12） */
    @Scheduled(cron = "0 0 9 * * ?")
    public void sendActivityReminders() {
        try {
            activityService.sendTomorrowReminders();
        } catch (Exception e) {
            log.error("活动提醒推送任务执行失败", e);
        }
    }

    /** 清理过期短信验证码（每 10 分钟，文档 5.12） */
    @Scheduled(cron = "0 0/10 * * * ?")
    public void cleanSmsCodes() {
        try {
            smsService.cleanExpiredCodes();
        } catch (Exception e) {
            log.error("短信验证码清理任务执行失败", e);
        }
    }
}
