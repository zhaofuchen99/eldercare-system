package com.zfc.eldercare.core.vo;

/**
 * 会员端问卷列表项（仅展示已发布问卷）。
 */
public record MemberQuestionnaireVO(
        Long id,
        String title,
        String description
) {
}
