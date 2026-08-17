package com.zfc.eldercare.core.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评测结果实体（对应 assessment_result 表，详细设计文档 6.3.7）。
 * answers：答案快照 JSON，结构见文档 6.3.7（version + items，含每题 value/score/max_score）
 * rule_score：规则分（百分制，仅计分题按选项分值加总后换算）
 * ai_score：AI 智能评分（百分制，最终展示分）
 */
@Data
public class AssessmentResult {

    private Long id;

    private Long userId;

    private Long questionnaireId;

    private String answers;

    private Integer ruleScore;

    private Integer aiScore;

    private String aiSuggestion;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer deleted;
}
