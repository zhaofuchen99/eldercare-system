package com.zfc.eldercare.core.vo;

/**
 * 评测报告单题展示（逐题展示所选选项的 text、meaning 与 score，详细设计文档 5.3 报告展示）。
 */
public record ResultItemVO(
        Long qid,
        String content,
        /** SINGLE/MULTIPLE/TEXT */
        String type,
        /** 用户所答内容：选项文案（多选为顿号拼接）或文本答案 */
        String answerText,
        /** 所选选项的标准语义（文本题为 null） */
        String meaning,
        /** 该题得分（仅计分题有值） */
        Integer score
) {
}
