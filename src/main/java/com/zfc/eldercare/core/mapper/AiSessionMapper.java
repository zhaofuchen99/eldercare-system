package com.zfc.eldercare.core.mapper;

import com.zfc.eldercare.core.entity.AiConversationSession;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 会话 Mapper（ai_conversation_session 表）。
 */
public interface AiSessionMapper {

    int insert(AiConversationSession session);

    AiConversationSession selectById(@Param("id") Long id);

    /** 某用户会话列表（按 update_time 倒序，最近对话置顶），配合 PageHelper 分页 */
    List<AiConversationSession> selectPageByUserId(@Param("userId") Long userId);

    /** 更新会话名称（首条消息后 AI 生成） */
    int updateName(@Param("id") Long id, @Param("sessionName") String sessionName);

    /** 触碰 update_time（新消息后保持"最近对话置顶"） */
    int touch(@Param("id") Long id);

    int delete(@Param("id") Long id);

    /** 物理清理过期会话（创建时间早于指定时间或已逻辑删除，文档 5.12 保留策略 6 个月） */
    int deleteExpired(@Param("beforeTime") LocalDateTime beforeTime);
}
