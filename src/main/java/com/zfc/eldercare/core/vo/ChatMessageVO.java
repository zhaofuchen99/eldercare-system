package com.zfc.eldercare.core.vo;

import java.time.LocalDateTime;

/**
 * AI 对话消息展示 VO。
 */
public record ChatMessageVO(
        Long id,
        /** USER/ASSISTANT */
        String role,
        String message,
        /** SUCCESS/FAILED */
        String status,
        LocalDateTime createTime
) {
}
