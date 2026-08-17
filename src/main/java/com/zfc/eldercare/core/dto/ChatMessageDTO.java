package com.zfc.eldercare.core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * AI 对话发送消息 DTO（详细设计文档 5.4）。
 */
public record ChatMessageDTO(

        @NotNull(message = "会话 ID 不能为空")
        Long sessionId,

        @NotBlank(message = "消息内容不能为空")
        @Size(max = 2000, message = "消息内容不能超过 2000 字")
        String content
) {
}
