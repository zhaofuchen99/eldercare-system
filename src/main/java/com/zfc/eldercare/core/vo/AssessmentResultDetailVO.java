package com.zfc.eldercare.core.vo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评测报告详情（详细设计文档 5.3 报告展示）。
 */
public record AssessmentResultDetailVO(
        Long id,
        Long questionnaireId,
        String questionnaireTitle,
        /** 规则分（仅计分题换算） */
        Integer ruleScore,
        /** AI 智能评分（最终展示分） */
        Integer aiScore,
        String aiSuggestion,
        /** 等级结论（按 grade_rules 匹配 ai_score） */
        String grade,
        LocalDateTime createTime,
        List<ResultItemVO> items
) {
}
