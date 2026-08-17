package com.zfc.eldercare.core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 批量消息推送 DTO（消息通知模块，管理端）。
 * 向多个用户推送同一条消息，单事务写入。
 */
public record MessageBatchPushDTO(

        @NotEmpty(message = "目标用户列表不能为空")
        List<@NotNull(message = "目标用户不能为空") Long> userIds,

        @NotBlank(message = "消息标题不能为空")
        @Size(max = 200, message = "消息标题不能超过 200 字")
        String title,

        @Size(max = 10000, message = "消息内容不能超过 10000 字")
        String content,

        @Pattern(regexp = "APPOINTMENT|ACTIVITY|SYSTEM|HEALTH_REMINDER", message = "消息类型只能是 APPOINTMENT/ACTIVITY/SYSTEM/HEALTH_REMINDER")
        String type
) {
}
