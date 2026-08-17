package com.zfc.eldercare.core.vo;

import com.zfc.eldercare.core.entity.Message;

import java.time.LocalDateTime;

/**
 * 站内消息 VO（消息通知模块，会员端/管理端通用）。
 */
public record MessageVO(
        Long id,
        Long userId,
        String title,
        String content,
        String type,
        Integer isRead,
        LocalDateTime createTime
) {
    public static MessageVO from(Message m) {
        return new MessageVO(m.getId(), m.getUserId(), m.getTitle(), m.getContent(),
                m.getType(), m.getIsRead(), m.getCreateTime());
    }
}
