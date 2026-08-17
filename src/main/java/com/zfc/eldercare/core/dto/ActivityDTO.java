package com.zfc.eldercare.core.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * 社区活动新增/更新 DTO（社区活动模块，管理端）。
 * 字段均可选；新增时 title 必填（服务层校验），其余缺省按默认值。
 */
public record ActivityDTO(

        @Size(max = 200, message = "活动标题不能超过 200 字")
        String title,

        @Size(max = 500, message = "封面图 URL 不能超过 500 字")
        String coverUrl,

        @Size(max = 10000, message = "活动内容不能超过 10000 字")
        String content,

        @Size(max = 200, message = "活动地点不能超过 200 字")
        String location,

        LocalDateTime registrationStartTime,

        LocalDateTime registrationEndTime,

        LocalDateTime activityStartTime,

        LocalDateTime activityEndTime,

        @Min(value = 1, message = "人数上限至少为 1")
        @Max(value = 100000, message = "人数上限超出范围")
        Integer maxParticipants,

        @Pattern(regexp = "DRAFT|REGISTRATING|IN_PROGRESS|ENDED", message = "状态只能是 DRAFT/REGISTRATING/IN_PROGRESS/ENDED")
        String status
) {
}
