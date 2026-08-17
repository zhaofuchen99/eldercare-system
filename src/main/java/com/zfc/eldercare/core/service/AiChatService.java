package com.zfc.eldercare.core.service;

import com.zfc.eldercare.core.dto.ChatMessageDTO;
import com.zfc.eldercare.core.vo.ChatMessageVO;
import com.zfc.eldercare.core.vo.ChatSessionVO;
import com.zfc.eldercare.core.vo.PageVO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI 对话服务（详细设计文档 5.4）。
 */
public interface AiChatService {

    /** 创建会话（session_name 置空，首条消息后生成） */
    Long createSession(Long userId);

    /** 会话列表（按 update_time 倒序） */
    PageVO<ChatSessionVO> sessions(Long userId, int page, int size);

    /** 删除会话（连同其全部消息逻辑删除） */
    void deleteSession(Long userId, Long sessionId);

    /** 会话消息历史（分页，倒序） */
    PageVO<ChatMessageVO> history(Long userId, Long sessionId, int page, int size);

    /** 发送消息（非流式）：保存用户消息与 AI 回复，返回 AI 回复内容 */
    String sendMessage(Long userId, ChatMessageDTO dto);

    /** 流式对话（SSE，打字机效果）：逐字推送，结束时持久化用户消息与 AI 回复 */
    SseEmitter streamMessage(Long userId, ChatMessageDTO dto);
}
