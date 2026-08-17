package com.zfc.eldercare.core.vo;

import com.zfc.eldercare.core.entity.ActivityRegistration;

import java.time.LocalDateTime;

/**
 * 活动报名记录 VO（管理端报名列表，含签到情况）。
 */
public record ActivityRegistrationVO(
        Long id,
        Long userId,
        String realName,
        String phone,
        String checkInStatus,
        LocalDateTime checkInTime,
        LocalDateTime createTime
) {
    public static ActivityRegistrationVO from(ActivityRegistration r, String realName, String phone) {
        return new ActivityRegistrationVO(r.getId(), r.getUserId(), realName, phone,
                r.getCheckInStatus(), r.getCheckInTime(), r.getCreateTime());
    }
}
