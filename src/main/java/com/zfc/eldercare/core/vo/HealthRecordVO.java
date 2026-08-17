package com.zfc.eldercare.core.vo;

import com.zfc.eldercare.core.entity.HealthRecord;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 健康记录展示 VO。
 */
public record HealthRecordVO(
        Long id,
        Integer systolic,
        Integer diastolic,
        BigDecimal bloodSugar,
        Integer heartRate,
        BigDecimal weight,
        BigDecimal bmi,
        String memo,
        LocalDateTime recordedTime
) {
    public static HealthRecordVO from(HealthRecord r) {
        return new HealthRecordVO(
                r.getId(), r.getSystolic(), r.getDiastolic(), r.getBloodSugar(),
                r.getHeartRate(), r.getWeight(), r.getBmi(), r.getMemo(), r.getRecordedTime());
    }
}
