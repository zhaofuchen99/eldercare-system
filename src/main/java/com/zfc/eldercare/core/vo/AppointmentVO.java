package com.zfc.eldercare.core.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 预约记录展示 VO（体检预约模块）。
 * reportDownloadUrl 为 5 分钟有效的签名下载链接，不暴露磁盘相对路径（文档 5.5 报告流程）。
 */
public record AppointmentVO(
        Long id,
        Long userId,
        Long slotId,
        Long packageId,
        String status,
        String packageName,
        Integer price,
        LocalDate appointDate,
        String timeRange,
        String originalFilename,
        LocalDateTime reportUploadTime,
        LocalDateTime createTime,
        String userName,
        String phone,
        String reportDownloadUrl
) {
}
