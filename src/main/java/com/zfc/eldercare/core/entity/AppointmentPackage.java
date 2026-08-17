package com.zfc.eldercare.core.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 体检套餐实体（对应 appointment_package 表，详细设计文档 6.3.8）。
 * status 编码：ENABLED启用/DISABLED禁用
 */
@Data
public class AppointmentPackage {

    private Long id;

    /** 套餐名称 */
    private String name;

    /** 封面图 URL */
    private String coverUrl;

    /** 套餐描述 */
    private String description;

    /** 价格（积分抵扣） */
    private Integer price;

    /** 适合人群 */
    private String suitablePeople;

    /** 包含项目列表 JSON，如 ["血常规","心电图"] */
    private String items;

    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer deleted;
}
