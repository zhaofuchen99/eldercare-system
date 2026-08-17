package com.zfc.eldercare.api.controller;

import com.zfc.eldercare.core.common.Result;
import com.zfc.eldercare.core.dto.ChatMessageDTO;
import com.zfc.eldercare.core.service.AiChatService;
import com.zfc.eldercare.core.vo.ChatMessageVO;
import com.zfc.eldercare.core.vo.ChatSessionVO;
import com.zfc.eldercare.core.vo.PageVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 会员端 AI 对话接口（/api/member/chat，详细设计文档 7.2）。
 */
@RestController
@RequestMapping("/api/member/chat")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;

    /** 创建会话 */
    @PostMapping("/session")
    public Result<Long> createSession(@AuthenticationPrincipal Long userId) {
        return Result.success("创建成功", aiChatService.createSession(userId));
    }

    /** 会话列表（最近对话置顶） */
    @GetMapping("/sessions")
    public Result<PageVO<ChatSessionVO>> sessions(@AuthenticationPrincipal Long userId,
                                                  @RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "10") int size) {
        return Result.success(aiChatService.sessions(userId, page, size));
    }

    /** 删除会话（连同全部消息） */
    @DeleteMapping("/session/{id}")
    public Result<Void> deleteSession(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        aiChatService.deleteSession(userId, id);
        return Result.success();
    }

    /** 会话消息历史 */
    @GetMapping("/session/{id}/messages")
    public Result<PageVO<ChatMessageVO>> history(@AuthenticationPrincipal Long userId,
                                                 @PathVariable Long id,
                                                 @RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "10") int size) {
        return Result.success(aiChatService.history(userId, id, page, size));
    }

    /** 发送消息（非流式，返回 AI 回复） */
    @PostMapping("/message")
    public Result<String> sendMessage(@AuthenticationPrincipal Long userId,
                                      @Valid @RequestBody ChatMessageDTO dto) {
        return Result.success(aiChatService.sendMessage(userId, dto));
    }

    /** 流式对话（SSE，打字机效果） */
    @PostMapping(value = "/message/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMessage(@AuthenticationPrincipal Long userId,
                                    @Valid @RequestBody ChatMessageDTO dto) {
        return aiChatService.streamMessage(userId, dto);
    }
}
