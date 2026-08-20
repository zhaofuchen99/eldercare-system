package com.zfc.eldercare.core.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.zfc.eldercare.core.dto.ChatMessageDTO;
import com.zfc.eldercare.core.entity.AiConversationMessage;
import com.zfc.eldercare.core.entity.AiConversationSession;
import com.zfc.eldercare.core.exception.BusinessException;
import com.zfc.eldercare.core.mapper.AiMessageMapper;
import com.zfc.eldercare.core.mapper.AiSessionMapper;
import com.zfc.eldercare.core.mapper.SysConfigMapper;
import com.zfc.eldercare.core.service.AiChatService;
import com.zfc.eldercare.core.service.AppointmentService;
import com.zfc.eldercare.core.service.HealthContextBuilder;
import com.zfc.eldercare.core.service.KnowledgeRetriever;
import com.zfc.eldercare.core.tool.AiAppointmentTools;
import com.zfc.eldercare.core.vo.ChatMessageVO;
import com.zfc.eldercare.core.vo.ChatSessionVO;
import com.zfc.eldercare.core.vo.PageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AI 对话服务实现（详细设计文档 5.4）。
 * 上下文取最近 10 轮（20 条消息）；SSE 逐字推送；断连/超时/错误均持久化不丢消息。
 */
@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements AiChatService {

    /** 系统提示词配置键 */
    private static final String KEY_SYSTEM_PROMPT = "ai_chat_system_prompt";
    /** 默认系统提示词（配置缺失时兜底） */
    private static final String DEFAULT_SYSTEM_PROMPT =
            "你是一位专业的健康顾问，请用亲切、易懂的语言回答用户的健康问题。";
    /** 最近对话轮数（10 轮 = 20 条消息，文档 5.4） */
    private static final int CONTEXT_MESSAGE_COUNT = 20;
    /** SSE 超时（毫秒）：文档要求 AI 接口超时 >60s 主动断开 */
    private static final long SSE_TIMEOUT_MS = 60_000L;

    private final ChatClient chatClient;
    private final AiSessionMapper aiSessionMapper;
    private final AiMessageMapper aiMessageMapper;
    private final SysConfigMapper sysConfigMapper;
    /** 用户健康数据增强：把会员健康档案注入 system 提示词，实现个性化健康咨询（AI 模块核心价值） */
    private final HealthContextBuilder healthContextBuilder;
    /** 养老知识库 RAG：命中才注入 user 上下文（fail-open，与健康咨询解耦） */
    private final KnowledgeRetriever knowledgeRetriever;
    /** 体检预约服务：供 AI 工具调用（工具调用，按次实例化绑定 userId，见 AiAppointmentTools） */
    private final AppointmentService appointmentService;

    @Override
    public Long createSession(Long userId) {
        AiConversationSession session = new AiConversationSession();
        session.setUserId(userId);
        aiSessionMapper.insert(session);
        return session.getId();
    }

    @Override
    public PageVO<ChatSessionVO> sessions(Long userId, int page, int size) {
        PageHelper.startPage(page, size);
        List<AiConversationSession> list = aiSessionMapper.selectPageByUserId(userId);
        PageInfo<AiConversationSession> pageInfo = new PageInfo<>(list);
        List<ChatSessionVO> voList = list.stream()
                .map(s -> new ChatSessionVO(s.getId(), s.getSessionName(), s.getCreateTime(), s.getUpdateTime()))
                .toList();
        return new PageVO<>(pageInfo.getPageNum(), pageInfo.getPageSize(),
                pageInfo.getTotal(), pageInfo.getPages(), voList);
    }

    @Override
    @Transactional
    public void deleteSession(Long userId, Long sessionId) {
        getOwnedSession(userId, sessionId);
        aiSessionMapper.delete(sessionId);
        aiMessageMapper.deleteBySessionId(sessionId);
    }

    @Override
    public PageVO<ChatMessageVO> history(Long userId, Long sessionId, int page, int size) {
        getOwnedSession(userId, sessionId);
        PageHelper.startPage(page, size);
        List<AiConversationMessage> list = aiMessageMapper.selectPageBySessionId(sessionId);
        PageInfo<AiConversationMessage> pageInfo = new PageInfo<>(list);
        List<ChatMessageVO> voList = list.stream()
                .map(m -> new ChatMessageVO(m.getId(), m.getRole(), m.getMessage(), m.getStatus(), m.getCreateTime()))
                .toList();
        return new PageVO<>(pageInfo.getPageNum(), pageInfo.getPageSize(),
                pageInfo.getTotal(), pageInfo.getPages(), voList);
    }

    @Override
    public String sendMessage(Long userId, ChatMessageDTO dto) {
        AiConversationSession session = getOwnedSession(userId, dto.sessionId());
        String context = buildContext(userId, session.getId(), dto.content());
        String reply;
        try {
            reply = chatClient.prompt()
                    .system(systemPrompt(userId))
                    .user(context)
                    .tools(new AiAppointmentTools(appointmentService, userId))
                    .call()
                    .content();
        } catch (RuntimeException e) {
            saveFailed(session, dto.content(), aiErrorMessage(e));
            throw new BusinessException(500, "AI 服务暂不可用，请稍后重试");
        }
        saveExchange(session, dto.content(), reply == null ? "" : reply);
        return reply;
    }

    @Override
    public SseEmitter streamMessage(Long userId, ChatMessageDTO dto) {
        AiConversationSession session = getOwnedSession(userId, dto.sessionId());
        String userContent = dto.content();
        String context = buildContext(userId, session.getId(), userContent);

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        StringBuilder full = new StringBuilder();
        AtomicBoolean saved = new AtomicBoolean(false);

        Flux<String> flux = chatClient.prompt()
                .system(systemPrompt(userId))
                .user(context)
                .tools(new AiAppointmentTools(appointmentService, userId))
                .stream()
                .content();

        flux.subscribe(
                chunk -> {
                    full.append(chunk);
                    try {
                        emitter.send(chunk);
                    } catch (IOException e) {
                        // 客户端中途断开：抛出进入 onError，把已接收内容保存为完整消息（不丢失，文档 5.4）
                        throw new RuntimeException(e);
                    }
                },
                error -> {
                    if (saved.compareAndSet(false, true)) {
                        if (!full.isEmpty()) {
                            // AI 接口超时/断连但有部分内容：保存已接收片段
                            saveExchange(session, userContent, full.toString());
                        } else {
                            // AI 接口错误：用户消息保存，AI 消息失败标记
                            saveFailed(session, userContent, aiErrorMessage(error));
                        }
                    }
                    completeEmitter(emitter, error);
                },
                () -> {
                    if (saved.compareAndSet(false, true)) {
                        saveExchange(session, userContent, full.toString());
                    }
                    emitter.complete();
                });

        return emitter;
    }

    @Override
    @Transactional
    public void cleanExpiredMessages() {
        // 保留策略：消息与会话均保留 6 个月（文档 6.9.3 / 5.12），超期及已逻辑删除的一并物理清理
        LocalDateTime beforeTime = LocalDateTime.now().minusMonths(6);
        aiMessageMapper.deleteExpired(beforeTime);
        aiSessionMapper.deleteExpired(beforeTime);
    }

    // ========== 私有辅助 ==========

    private AiConversationSession getOwnedSession(Long userId, Long sessionId) {
        AiConversationSession session = aiSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException(404, "会话不存在");
        }
        if (!session.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权访问该会话");
        }
        return session;
    }

    /** 最近 10 轮上下文：知识库参考（命中才注入） + 历史 + 当前问题 */
    private String buildContext(Long userId, Long sessionId, String userContent) {
        List<AiConversationMessage> recent = aiMessageMapper.selectRecentBySessionId(sessionId, CONTEXT_MESSAGE_COUNT);
        StringBuilder sb = new StringBuilder();
        String knowledge = knowledgeRetriever.retrieve(userContent);
        if (StringUtils.hasText(knowledge)) {
            sb.append("【知识库参考】以下资料仅供回答参考，请用口语化方式向用户传达：\n")
              .append(knowledge).append("\n\n");
        }
        for (int i = recent.size() - 1; i >= 0; i--) {
            AiConversationMessage m = recent.get(i);
            sb.append("USER".equals(m.getRole()) ? "用户" : "助手")
              .append("：").append(m.getMessage()).append("\n");
        }
        sb.append("用户：").append(userContent);
        return sb.toString();
    }

    /** 成功交互落库：用户消息 + 助手消息 + 触碰会话时间 + 首条消息生成会话名称 */
    private void saveExchange(AiConversationSession session, String userContent, String assistantContent) {
        insertMessage(session, "USER", userContent, "SUCCESS");
        insertMessage(session, "ASSISTANT", assistantContent, "SUCCESS");
        aiSessionMapper.touch(session.getId());
        maybeNameSession(session, userContent);
    }

    /** AI 失败落库：用户消息正常保存，助手消息打失败标记（文档 5.4） */
    private void saveFailed(AiConversationSession session, String userContent, String errorMsg) {
        insertMessage(session, "USER", userContent, "SUCCESS");
        insertMessage(session, "ASSISTANT", errorMsg, "FAILED");
        aiSessionMapper.touch(session.getId());
    }

    private void insertMessage(AiConversationSession session, String role, String content, String status) {
        AiConversationMessage msg = new AiConversationMessage();
        msg.setSessionId(session.getId());
        msg.setUserId(session.getUserId());
        msg.setRole(role);
        msg.setMessage(content);
        msg.setStatus(status);
        aiMessageMapper.insert(msg);
    }

    /** 首条消息后生成会话名称；AI 生成失败/超时则取首条消息前 20 字兜底（文档 5.4） */
    private void maybeNameSession(AiConversationSession session, String firstUserContent) {
        if (session.getSessionName() != null) {
            return;
        }
        String name = null;
        try {
            name = chatClient.prompt()
                    .user("请为以下用户问题生成一个不超过 12 个字的简短会话标题，只返回标题本身，不要引号和多余标点：\n"
                            + firstUserContent)
                    .call()
                    .content();
        } catch (RuntimeException ignored) {
            // 生成失败走兜底
        }
        if (name == null || name.isBlank()) {
            name = firstUserContent.length() > 20 ? firstUserContent.substring(0, 20) : firstUserContent;
        } else {
            name = name.trim();
            if (name.length() > 100) {
                name = name.substring(0, 100);
            }
        }
        aiSessionMapper.updateName(session.getId(), name);
        session.setSessionName(name);
    }

    /** 系统提示词：基础人设 + 用户健康档案增强 + 工具调用约束（健康数据仅内部参考、不外泄） */
    private String systemPrompt(Long userId) {
        String base = sysConfigMapper.selectValueByKey(KEY_SYSTEM_PROMPT);
        base = StringUtils.hasText(base) ? base : DEFAULT_SYSTEM_PROMPT;
        String health = healthContextBuilder.build(userId);
        if (StringUtils.hasText(health)) {
            base += "\n\n请结合以下用户健康档案给出个性化建议（仅供内部参考，不要向用户复述档案原文）：\n" + health;
        }
        // 工具调用（体检预约）行为约束：预约会扣除会员积分，必须确认后执行
        base += "\n\n【可执行能力——体检预约】你可以帮助会员完成体检预约（会员已登录，无需询问身份）：\n"
                + "1. 先调用 queryPackages 展示可预约套餐（含价格/积分、适合人群），询问用户选择哪个；\n"
                + "2. 用户选定套餐后，调用 querySlots 查询可预约日期与时段，向用户展示可选时段；\n"
                + "3. 预约会扣除会员积分，必须先向用户说明所选时段与将扣除的积分，并等待用户明确确认\n"
                + "   （如“好的/确认/可以”）后才可调用 bookAppointment，且 confirm 参数必须传 true；\n"
                + "4. 预约结果要如实转告用户（成功或失败原因）。\n";
        return base;
    }

    private String aiErrorMessage(Throwable error) {
        String msg = error.getMessage();
        if (!StringUtils.hasText(msg)) {
            msg = error.getClass().getSimpleName();
        }
        return msg.length() > 500 ? msg.substring(0, 500) : msg;
    }

    private void completeEmitter(SseEmitter emitter, Throwable error) {
        try {
            emitter.completeWithError(error);
        } catch (Exception ignored) {
            // 客户端可能已断开，忽略
        }
    }
}
