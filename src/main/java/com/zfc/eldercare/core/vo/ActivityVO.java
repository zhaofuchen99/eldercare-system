package com.zfc.eldercare.core.vo;

import com.zfc.eldercare.core.entity.CommunityActivity;

import java.time.LocalDateTime;

/**
 * 社区活动展示 VO。
 */
public record ActivityVO(
        Long id,
        String title,
        String coverUrl,
        String content,
        String location,
        LocalDateTime registrationStartTime,
        LocalDateTime registrationEndTime,
        LocalDateTime activityStartTime,
        LocalDateTime activityEndTime,
        Integer maxParticipants,
        Integer currentParticipants,
        String status,
        LocalDateTime createTime
) {
    public static ActivityVO from(CommunityActivity a) {
        return new ActivityVO(a.getId(), a.getTitle(), a.getCoverUrl(), a.getContent(), a.getLocation(),
                a.getRegistrationStartTime(), a.getRegistrationEndTime(), a.getActivityStartTime(),
                a.getActivityEndTime(), a.getMaxParticipants(), a.getCurrentParticipants(),
                a.getStatus(), a.getCreateTime());
    }
}
