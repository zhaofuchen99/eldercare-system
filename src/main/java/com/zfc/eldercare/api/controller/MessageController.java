package com.zfc.eldercare.api.controller;

import com.zfc.eldercare.core.common.Result;
import com.zfc.eldercare.core.service.MessageService;
import com.zfc.eldercare.core.vo.MessageUnreadVO;
import com.zfc.eldercare.core.vo.MessageVO;
import com.zfc.eldercare.core.vo.PageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 会员端消息通知接口（/api/member/message，详细设计文档 5.7 / 7.2）。
 * 列表、详情、标记已读、未读统计。
 */
@RestController
@RequestMapping("/api/member/message")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    /** 消息列表（分页，未读在前） */
    @GetMapping
    public Result<PageVO<MessageVO>> list(@AuthenticationPrincipal Long userId,
                                          @RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "10") int size) {
        return Result.success(messageService.userMessages(userId, page, size));
    }

    /** 未读统计（首页角标用） */
    @GetMapping("/unread-count")
    public Result<MessageUnreadVO> unreadCount(@AuthenticationPrincipal Long userId) {
        return Result.success(messageService.unreadStats(userId));
    }

    /** 消息详情 */
    @GetMapping("/{id}")
    public Result<MessageVO> detail(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        return Result.success(messageService.messageDetail(userId, id));
    }

    /** 标记已读 */
    @PutMapping("/{id}/read")
    public Result<Void> read(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        messageService.markRead(userId, id);
        return Result.success("操作成功", null);
    }
}
