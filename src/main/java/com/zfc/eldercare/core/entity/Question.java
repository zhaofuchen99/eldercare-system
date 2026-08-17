package com.zfc.eldercare.core.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 题目实体（对应 question 表，详细设计文档 6.3.6）。
 * type 编码：SINGLE单选/MULTIPLE多选/TEXT文本
 * score_mode 编码：SCORED计分（选项分值参与规则分）/NON_SCORED非计分（不参与规则分，由 AI 语义评估）
 * options：选项 JSON 数组，计分题每项含 text+meaning+score，非计分题仅含 text+meaning，文本题为 NULL
 */
@Data
public class Question {

    private Long id;

    private Long questionnaireId;

    private String content;

    private String type;

    private String options;

    private String scoreMode;

    /** 题目满分（计分题计入问卷 total_score；文本题为 AI 评估分值上限，不参与规则分） */
    private Integer maxScore;

    private Integer sortOrder;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer deleted;
}
