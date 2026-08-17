package com.zfc.eldercare.core.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 体检预约实体（对应 appointment 表，详细设计文档 6.3.10）。
 * status 编码：PENDING待确认/CONFIRMED已确认/CANCELED已取消/COMPLETED已完成
 */
@Data
public class Appointment {

    private Long id;

    private Long userId;

    /** 时间段 ID */
    private Long slotId;

    /** 套餐 ID */
    private Long packageId;

    private String status;

    /** 体检报告相对路径（如 report/202608/{uuid}.pdf） */
    private String reportUrl;

    /** 体检报告原始文件名 */
    private String originalFilename;

    /** 报告上传时间 */
    private LocalDateTime reportUploadTime;

    /** 上传管理员用户 ID */
    private Long uploadAdminId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer deleted;
}
