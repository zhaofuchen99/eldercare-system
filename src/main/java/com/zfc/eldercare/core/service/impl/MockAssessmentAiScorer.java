package com.zfc.eldercare.core.service.impl;

import com.zfc.eldercare.core.service.AssessmentAiScorer;
import org.springframework.stereotype.Service;

/**
 * AI 评分 Mock 实现：ai_score 直接取规则分，建议为通用模板文案。
 * 待 5.4 AI 对话模块引入 spring-ai（DeepSeek）后，新增真实实现并替换本类。
 */
@Service
public class MockAssessmentAiScorer implements AssessmentAiScorer {

    @Override
    public AiScoreResult score(String prompt, int ruleScore) {
        String suggestion = "根据您的健康评测结果（基础评分 " + ruleScore + " 分），"
                + "建议您保持规律作息、均衡饮食与适量运动，并定期关注自身健康指标变化。"
                + "（当前为系统基础评分，接入 AI 后将为您提供个性化健康建议）";
        return new AiScoreResult(ruleScore, suggestion);
    }
}
