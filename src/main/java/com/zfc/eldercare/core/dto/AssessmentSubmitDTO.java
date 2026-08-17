package com.zfc.eldercare.core.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 评测提交 DTO（详细设计文档 5.3）。
 */
public record AssessmentSubmitDTO(

        @NotNull(message = "问卷 ID 不能为空")
        Long questionnaireId,

        @NotEmpty(message = "答案不能为空")
        @Valid
        List<AnswerItemDTO> items
) {
    /** 单题答案：value 为选项下标/下标数组/文本 */
    public record AnswerItemDTO(
            @NotNull(message = "题目 ID 不能为空")
            Long qid,
            @NotNull(message = "答案不能为空")
            Object value
    ) {
    }
}
