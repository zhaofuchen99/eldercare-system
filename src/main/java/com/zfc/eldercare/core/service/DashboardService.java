package com.zfc.eldercare.core.service;

import com.zfc.eldercare.core.vo.DashboardVO;

/**
 * 仪表盘服务（详细设计文档 5.9）。
 */
public interface DashboardService {

    /** 数据概览统计（各指标直接 COUNT 实时统计，无需预聚合） */
    DashboardVO overview();
}
