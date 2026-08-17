package com.zfc.eldercare.core.vo;

import java.util.List;

/**
 * 会员端问卷详情（含题目，用于答题）。
 */
public record MemberQuestionnaireDetailVO(
        Long id,
        String title,
        String description,
        List<MemberQuestionVO> questions
) {
}
