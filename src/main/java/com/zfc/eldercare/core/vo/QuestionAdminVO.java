package com.zfc.eldercare.core.vo;

import java.util.List;

/**
 * 管理端题目展示 VO（含完整选项：文案+语义+分值）。
 */
public record QuestionAdminVO(
        Long id,
        String content,
        /** SINGLE/MULTIPLE/TEXT */
        String type,
        /** SCORED/NON_SCORED */
        String scoreMode,
        List<QuestionOptionVO> options,
        Integer maxScore,
        Integer sortOrder
) {
}
