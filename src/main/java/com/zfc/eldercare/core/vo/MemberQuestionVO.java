package com.zfc.eldercare.core.vo;

import java.util.List;

/**
 * 会员端答题页单题（仅暴露选项文案，不暴露分值/标准语义，避免影响作答）。
 */
public record MemberQuestionVO(
        Long id,
        String content,
        /** SINGLE/MULTIPLE/TEXT */
        String type,
        /** 选项文案列表，文本题为空 */
        List<String> options
) {
}
