package com.zfc.eldercare.api.controller;

import com.zfc.eldercare.core.common.Result;
import com.zfc.eldercare.core.service.HealthRecordService;
import com.zfc.eldercare.core.vo.HealthRecordVO;
import com.zfc.eldercare.core.vo.HealthTrendVO;
import com.zfc.eldercare.core.vo.PageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端健康档案接口（/api/admin/health-record，详细设计文档 5.2 管理端职责 / 7.2）。
 */
@RestController
@RequestMapping("/api/admin/health-record")
@RequiredArgsConstructor
public class AdminHealthRecordController {

    private final HealthRecordService healthRecordService;

    /** 按会员分页查询健康记录 */
    @GetMapping
    public Result<PageVO<HealthRecordVO>> page(@RequestParam Long userId,
                                               @RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "10") int size) {
        return Result.success(healthRecordService.pageByUser(userId, page, size));
    }

    /** 某会员指定指标近 6 个月趋势 */
    @GetMapping("/trend")
    public Result<HealthTrendVO> trend(@RequestParam Long userId,
                                       @RequestParam(required = false) String indicator) {
        return Result.success(healthRecordService.trend(userId, indicator));
    }
}
