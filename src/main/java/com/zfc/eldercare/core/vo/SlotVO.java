package com.zfc.eldercare.core.vo;

import com.zfc.eldercare.core.entity.AppointmentSlot;

import java.time.LocalDate;

/**
 * 预约时段展示 VO（体检预约模块）。
 */
public record SlotVO(
        Long id,
        Long packageId,
        String packageName,
        LocalDate appointDate,
        String timeRange,
        Integer maxCount,
        Integer currentCount,
        String status
) {
    public static SlotVO from(AppointmentSlot slot, String packageName) {
        return new SlotVO(slot.getId(), slot.getPackageId(), packageName, slot.getAppointDate(),
                slot.getTimeRange(), slot.getMaxCount(), slot.getCurrentCount(), slot.getStatus());
    }
}
