package com.zfc.eldercare.core.vo;

import java.time.LocalDateTime;

/**
 * 评测历史列表项。
 */
public record AssessmentResultListVO(
        Long id,
        Long questionnaireId,
        String questionnaireTitle,
        Integer ruleScore,
        Integer aiScore,
        LocalDateTime createTime
) {
}
