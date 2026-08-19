package com.zfc.eldercare.core.service.impl;

import com.zfc.eldercare.core.mapper.SysConfigMapper;
import com.zfc.eldercare.core.service.AssessmentAiScorer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DeepSeek 健康评测智能评分（AI 模块 - 5.3 智能评分真实接入）。
 * 替换原 MockAssessmentAiScorer：让 DeepSeek 结合问卷答案输出 0-100 评分与个性化建议。
 * AI 调用/输出解析失败时回退规则分（等价原 Mock 行为，未配 Key 也能降级可用）。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeepSeekAssessmentAiScorer implements AssessmentAiScorer {

    /** 评分系统提示词配置键 */
    private static final String KEY_SYSTEM_PROMPT = "ai_assessment_system_prompt";
    /** 默认评分系统提示词（配置缺失时兜底） */
    private static final String DEFAULT_SYSTEM_PROMPT =
            "你是一位资深的健康评测专家。请结合问卷答案与规则分，给出 0-100 整数 AI 评分和不超过 200 字的个性化健康建议。"
                    + "只输出 JSON：{\"aiScore\":<0-100整数>,\"suggestion\":\"<建议>\"}";

    /** 正则兜底提取（JSON 解析失败时用） */
    private static final Pattern AI_SCORE_PATTERN = Pattern.compile("\"aiScore\"\\s*:\\s*(\\d{1,3})");
    private static final Pattern SUGGESTION_PATTERN = Pattern.compile("\"suggestion\"\\s*:\\s*\"([^\"]{0,300})\"");

    private final ChatClient chatClient;
    private final SysConfigMapper sysConfigMapper;
    private final ObjectMapper objectMapper;

    @Override
    public AiScoreResult score(String prompt, int ruleScore) {
        try {
            String system = sysConfigMapper.selectValueByKey(KEY_SYSTEM_PROMPT);
            if (!StringUtils.hasText(system)) {
                system = DEFAULT_SYSTEM_PROMPT;
            }
            String resp = chatClient.prompt()
                    .system(system)
                    .user(prompt + "\n（规则分：" + ruleScore + "）")
                    .call()
                    .content();
            return parse(resp, ruleScore);
        } catch (RuntimeException e) {
            log.warn("AI 评分调用失败，回退规则分：{}", e.getMessage());
            return fallback(ruleScore);
        }
    }

    /** 解析模型输出：去代码围栏 → JSON 解析 → 正则兜底；评分 clamp 0-100，失败回退规则分 */
    private AiScoreResult parse(String resp, int ruleScore) {
        if (resp == null) {
            return fallback(ruleScore);
        }
        String json = resp.trim()
                .replaceAll("^```[a-zA-Z]*\\s*", "")
                .replaceAll("\\s*```$", "");
        try {
            Map<String, Object> map = objectMapper.readValue(json, new TypeReference<>() {
            });
            int aiScore = ruleScore;
            Object scoreObj = map.get("aiScore");
            if (scoreObj != null) {
                aiScore = clamp(Integer.parseInt(String.valueOf(scoreObj).trim()));
            }
            Object suggestion = map.get("suggestion");
            return new AiScoreResult(aiScore, suggestion == null ? "" : String.valueOf(suggestion));
        } catch (Exception e) {
            Matcher scoreMatcher = AI_SCORE_PATTERN.matcher(json);
            Matcher suggMatcher = SUGGESTION_PATTERN.matcher(json);
            if (scoreMatcher.find()) {
                int aiScore = clamp(Integer.parseInt(scoreMatcher.group(1)));
                String suggestion = suggMatcher.find() ? suggMatcher.group(1) : "";
                return new AiScoreResult(aiScore, suggestion);
            }
            log.warn("AI 评分输出解析失败，回退规则分：{}", resp);
            return fallback(ruleScore);
        }
    }

    private int clamp(int score) {
        return Math.min(100, Math.max(0, score));
    }

    private AiScoreResult fallback(int ruleScore) {
        String suggestion = "根据您的健康评测结果（基础评分 " + ruleScore + " 分），建议您保持规律作息、"
                + "合理饮食与适度运动，并定期关注自身健康指标。";
        return new AiScoreResult(ruleScore, suggestion);
    }
}
