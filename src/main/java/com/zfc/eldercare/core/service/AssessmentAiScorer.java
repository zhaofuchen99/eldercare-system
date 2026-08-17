package com.zfc.eldercare.core.service;

/**
 * 健康评测 AI 评分器（详细设计文档 5.3）。
 * 当前为 Mock 实现（ai_score 回退为规则分），接入 DeepSeek 后替换实现类即可。
 */
public interface AssessmentAiScorer {

    /**
     * 基于问卷上下文与规则分评定 AI 分数与建议。
     *
     * @param prompt    拼装好的问题上下文（问卷信息 + 全部题目与答案，含选项标准语义）
     * @param ruleScore 规则分（百分制）
     */
    AiScoreResult score(String prompt, int ruleScore);

    record AiScoreResult(int aiScore, String suggestion) {
    }
}
