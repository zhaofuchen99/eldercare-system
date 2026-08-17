package com.zfc.eldercare.core.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 对话消息实体（对应 ai_conversation_message 表，详细设计文档 6.3.15）。
 * role 编码：USER用户/ASSISTANT助手
 * status 编码：SUCCESS成功/FAILED失败（AI 接口错误时持久化失败标记）
 */
@Data
public class AiConversationMessage {

    private Long id;

    private Long sessionId;

    private Long userId;

    private String role;

    private String message;

    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer deleted;
}
