package com.zfc.eldercare.core.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 题目创建/编辑 DTO（管理端，详细设计文档 5.3 管理端职责）。
 * id 为 null 表示新增，否则为编辑。
 */
public record QuestionDTO(

        Long id,

        @NotBlank(message = "题目内容不能为空")
        String content,

        /** SINGLE/MULTIPLE/TEXT */
        @NotBlank(message = "题目类型不能为空")
        String type,

        /** SCORED/NON_SCORED */
        @NotBlank(message = "计分模式不能为空")
        String scoreMode,

        /** 计分题每项含 text+meaning+score，非计分题仅含 text+meaning，文本题传 null/空 */
        @Valid
        List<OptionDTO> options,

        @NotNull(message = "题目满分不能为空")
        Integer maxScore,

        Integer sortOrder
) {
    /** 选项：计分题每项含 text+meaning+score，非计分题仅含 text+meaning */
    public record OptionDTO(
            @NotBlank(message = "选项文案不能为空")
            String text,
            String meaning,
            Integer score
    ) {
    }
}
