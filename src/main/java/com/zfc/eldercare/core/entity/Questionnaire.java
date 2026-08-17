package com.zfc.eldercare.core.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 问卷实体（对应 questionnaire 表，详细设计文档 6.3.5）。
 * status 编码：DRAFT草稿/PUBLISHED已发布
 * grade_rules：评分等级规则 JSON，如 [{"min":90,"label":"优秀","description":"..."}]
 */
@Data
public class Questionnaire {

    private Long id;

    private String title;

    private String description;

    private String status;

    /** 问卷满分（仅计分题 max_score 之和，用于规则分百分制换算） */
    private Integer totalScore;

    /** 及格分数线（百分制） */
    private Integer passScore;

    /** 评分等级规则 JSON */
    private String gradeRules;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer deleted;
}
