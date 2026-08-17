package com.zfc.eldercare.core.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 社区活动实体（对应 community_activity 表，详细设计文档 6.3.11）。
 * status 编码：DRAFT草稿/REGISTRATING报名中/IN_PROGRESS进行中/ENDED已结束
 */
@Data
public class CommunityActivity {

    private Long id;

    /** 活动标题 */
    private String title;

    /** 封面图 URL */
    private String coverUrl;

    /** 活动内容 */
    private String content;

    /** 活动地点 */
    private String location;

    /** 报名开始时间 */
    private LocalDateTime registrationStartTime;

    /** 报名结束时间 */
    private LocalDateTime registrationEndTime;

    /** 活动开始时间 */
    private LocalDateTime activityStartTime;

    /** 活动结束时间 */
    private LocalDateTime activityEndTime;

    /** 人数上限（NULL 表示不限） */
    private Integer maxParticipants;

    /** 当前报名人数 */
    private Integer currentParticipants;

    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer deleted;
}
