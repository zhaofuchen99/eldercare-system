package com.zfc.eldercare.api.controller;

import com.zfc.eldercare.core.common.Result;
import com.zfc.eldercare.core.service.DashboardService;
import com.zfc.eldercare.core.vo.DashboardVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端仪表盘接口（/api/admin/dashboard，详细设计文档 5.9 / 7.2）。
 * 系统数据概览统计。
 */
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final DashboardService dashboardService;

    /** 数据概览统计 */
    @GetMapping
    public Result<DashboardVO> overview() {
        return Result.success(dashboardService.overview());
    }
}
