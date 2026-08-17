package com.zfc.eldercare.core.vo;

import java.util.List;
import java.util.Map;

/**
 * 健康趋势分析结果（详细设计文档 5.2）。
 * key 为指标编码（SYSTOLIC/DIASTOLIC/BLOOD_SUGAR/HEART_RATE/BMI），
 * value 为近 6 个月按月的平均值/最大值/最小值。
 */
public record HealthTrendVO(
        Map<String, List<TrendPointVO>> data
) {
}
