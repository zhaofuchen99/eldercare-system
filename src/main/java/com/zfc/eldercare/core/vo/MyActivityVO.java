package com.zfc.eldercare.core.vo;

import com.zfc.eldercare.core.entity.ActivityRegistration;
import com.zfc.eldercare.core.entity.CommunityActivity;

import java.time.LocalDateTime;

/**
 * 我的活动 VO（会员端，报名记录 + 活动信息 + 签到状态）。
 */
public record MyActivityVO(
        Long id,
        Long activityId,
        String title,
        String location,
        LocalDateTime activityStartTime,
        LocalDateTime activityEndTime,
        String activityStatus,
        String checkInStatus,
        LocalDateTime checkInTime,
        LocalDateTime createTime
) {
    public static MyActivityVO from(ActivityRegistration r, CommunityActivity activity) {
        return new MyActivityVO(r.getId(), r.getActivityId(),
                activity == null ? null : activity.getTitle(),
                activity == null ? null : activity.getLocation(),
                activity == null ? null : activity.getActivityStartTime(),
                activity == null ? null : activity.getActivityEndTime(),
                activity == null ? null : activity.getStatus(),
                r.getCheckInStatus(), r.getCheckInTime(), r.getCreateTime());
    }
}
