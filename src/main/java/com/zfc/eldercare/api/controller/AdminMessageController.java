package com.zfc.eldercare.api.controller;

import com.zfc.eldercare.core.common.Result;
import com.zfc.eldercare.core.dto.MessageBatchPushDTO;
import com.zfc.eldercare.core.dto.MessagePushDTO;
import com.zfc.eldercare.core.service.MessageService;
import com.zfc.eldercare.core.vo.MessageVO;
import com.zfc.eldercare.core.vo.PageVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端消息管理接口（/api/admin/message，详细设计文档 5.7 / 7.2）。
 * 消息分页、详情、推送单条、批量推送、删除。
 */
@RestController
@RequestMapping("/api/admin/message")
@RequiredArgsConstructor
public class AdminMessageController {

    private final MessageService messageService;

    /** 消息分页（可按 userId/type 筛选） */
    @GetMapping("/page")
    public Result<PageVO<MessageVO>> page(@RequestParam(required = false) Long userId,
                                          @RequestParam(required = false) String type,
                                          @RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "10") int size) {
        return Result.success(messageService.messagePage(userId, type, page, size));
    }

    /** 消息详情（管理端） */
    @GetMapping("/{id}")
    public Result<MessageVO> detail(@PathVariable Long id) {
        return Result.success(messageService.adminMessageDetail(id));
    }

    /** 推送单条消息 */
    @PostMapping
    public Result<Long> push(@Valid @RequestBody MessagePushDTO dto) {
        return Result.success("推送成功", messageService.pushMessage(dto));
    }

    /** 批量推送消息（向多个用户推送同一条） */
    @PostMapping("/batch")
    public Result<Integer> pushBatch(@Valid @RequestBody MessageBatchPushDTO dto) {
        return Result.success("推送成功", messageService.pushBatch(dto));
    }

    /** 删除消息（逻辑删除） */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        messageService.deleteMessage(id);
        return Result.success("删除成功", null);
    }
}
