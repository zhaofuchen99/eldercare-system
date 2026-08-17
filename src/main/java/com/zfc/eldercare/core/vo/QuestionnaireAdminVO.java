package com.zfc.eldercare.core.vo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端问卷展示 VO。
 */
public record QuestionnaireAdminVO(
        Long id,
        String title,
        String description,
        /** DRAFT/PUBLISHED */
        String status,
        Integer totalScore,
        Integer passScore,
        List<GradeRuleVO> gradeRules,
        LocalDateTime createTime
) {
}
