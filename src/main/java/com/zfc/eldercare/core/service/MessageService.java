package com.zfc.eldercare.core.service;

import com.zfc.eldercare.core.dto.MessageBatchPushDTO;
import com.zfc.eldercare.core.dto.MessagePushDTO;
import com.zfc.eldercare.core.vo.MessageUnreadVO;
import com.zfc.eldercare.core.vo.MessageVO;
import com.zfc.eldercare.core.vo.PageVO;

/**
 * 站内消息服务（详细设计文档 5.7）。
 */
public interface MessageService {

    // ========== 会员端 ==========

    /** 我的消息分页（未读在前，新的在前） */
    PageVO<MessageVO> userMessages(Long userId, int page, int size);

    /** 消息详情（校验归属，非本人 403） */
    MessageVO messageDetail(Long userId, Long id);

    /** 标记已读（校验归属） */
    void markRead(Long userId, Long id);

    /** 未读统计（总条数 + 未读数） */
    MessageUnreadVO unreadStats(Long userId);

    // ========== 管理端 ==========

    /** 消息分页（可按用户/类型筛选） */
    PageVO<MessageVO> messagePage(Long userId, String type, int page, int size);

    /** 消息详情（管理端） */
    MessageVO adminMessageDetail(Long id);

    /** 推送单条消息（校验目标用户存在） */
    Long pushMessage(MessagePushDTO dto);

    /** 批量推送消息（校验目标用户均存在，单事务），返回推送条数 */
    int pushBatch(MessageBatchPushDTO dto);

    /** 删除消息（逻辑删除） */
    void deleteMessage(Long id);
}
