package com.zfc.eldercare.core.vo;

/**
 * 评分等级规则项（问卷 grade_rules JSON 中的一项，详细设计文档 6.3.5）。
 * 按 ai_score 百分制区间判定：ai_score >= min 即命中（min 越大优先级越高）。
 */
public record GradeRuleVO(
        Integer min,
        String label,
        String description
) {
}
