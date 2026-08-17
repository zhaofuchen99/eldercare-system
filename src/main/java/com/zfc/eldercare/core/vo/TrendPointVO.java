package com.zfc.eldercare.core.vo;

import java.math.BigDecimal;

/**
 * 单指标某月的趋势统计点（详细设计文档 5.2 趋势分析）。
 */
public record TrendPointVO(
        /** 月份，格式 yyyy-MM */
        String month,
        BigDecimal avg,
        BigDecimal max,
        BigDecimal min
) {
}
