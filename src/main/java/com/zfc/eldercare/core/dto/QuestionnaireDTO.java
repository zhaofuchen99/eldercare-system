package com.zfc.eldercare.core.dto;

import com.zfc.eldercare.core.vo.GradeRuleVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 问卷创建/编辑 DTO（管理端，详细设计文档 5.3 管理端职责）。
 */
public record QuestionnaireDTO(

        @NotBlank(message = "问卷标题不能为空")
        String title,

        String description,

        @NotNull(message = "问卷满分不能为空")
        Integer totalScore,

        @NotNull(message = "及格分数线不能为空")
        Integer passScore,

        /** 评分等级规则，如 [{min:90,label:"优秀",...}]，序列化为 JSON 存 grade_rules */
        @Valid
        List<GradeRuleVO> gradeRules
) {
}
