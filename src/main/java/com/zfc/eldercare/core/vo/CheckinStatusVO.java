package com.zfc.eldercare.core.vo;

import java.time.LocalDateTime;

/**
 * 签到状态 VO（会员端，活动详情页展示当前用户报名/签到情况）。
 */
public record CheckinStatusVO(
        Long activityId,
        boolean registered,
        String checkInStatus,
        LocalDateTime checkInTime
) {
}
