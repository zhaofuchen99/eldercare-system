package com.zfc.eldercare.core.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 会话实体（对应 ai_conversation_session 表，详细设计文档 6.3.14）。
 * session_name 创建时为空，首条消息后由 AI 生成（失败则取首条消息前 20 字兜底）。
 */
@Data
public class AiConversationSession {

    private Long id;

    private Long userId;

    private String sessionName;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer deleted;
}
