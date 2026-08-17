package com.zfc.eldercare.core.vo;

/**
 * 消息统计 VO（会员端，未读统计）。
 */
public record MessageUnreadVO(
        long total,
        long unread
) {
}
