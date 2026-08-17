package com.zfc.eldercare.core.vo;

import java.time.LocalDateTime;

/**
 * AI 会话展示 VO。
 */
public record ChatSessionVO(
        Long id,
        String sessionName,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}
