package com.zfc.eldercare.core.mapper;

import com.zfc.eldercare.core.entity.AiConversationMessage;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 对话消息 Mapper（ai_conversation_message 表）。
 */
public interface AiMessageMapper {

    int insert(AiConversationMessage message);

    /** 某会话消息列表（倒序），配合 PageHelper 分页 */
    List<AiConversationMessage> selectPageBySessionId(@Param("sessionId") Long sessionId);

    /** 最近 N 条消息（倒序取回，服务层反转成时间正序用于上下文） */
    List<AiConversationMessage> selectRecentBySessionId(@Param("sessionId") Long sessionId, @Param("limit") int limit);

    /** 会话删除时一并逻辑删除其全部消息（文档 5.4） */
    int deleteBySessionId(@Param("sessionId") Long sessionId);

    /** 物理清理过期消息（创建时间早于指定时间或已逻辑删除，文档 5.12 保留策略 6 个月） */
    int deleteExpired(@Param("beforeTime") LocalDateTime beforeTime);
}
