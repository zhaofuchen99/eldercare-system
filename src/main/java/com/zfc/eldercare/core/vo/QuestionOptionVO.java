package com.zfc.eldercare.core.vo;

/**
 * 题目选项（管理端题目管理 / 评测报告展示，详细设计文档 6.3.6）。
 */
public record QuestionOptionVO(
        String text,
        /** 标准语义文本（由管理员配置，AI 不得另行解读） */
        String meaning,
        /** 计分题的分值，非计分题为 null */
        Integer score
) {
}
