package com.zfc.eldercare.core.vo;

import com.zfc.eldercare.core.entity.AppointmentPackage;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 体检套餐展示 VO（体检预约模块）。
 */
public record PackageVO(
        Long id,
        String name,
        String coverUrl,
        String description,
        Integer price,
        String suitablePeople,
        List<String> items,
        String status,
        LocalDateTime createTime
) {
    public static PackageVO from(AppointmentPackage pkg, List<String> items) {
        return new PackageVO(pkg.getId(), pkg.getName(), pkg.getCoverUrl(), pkg.getDescription(),
                pkg.getPrice(), pkg.getSuitablePeople(), items, pkg.getStatus(), pkg.getCreateTime());
    }
}
