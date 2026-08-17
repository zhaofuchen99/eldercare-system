package com.zfc.eldercare.api.controller;

import com.zfc.eldercare.core.common.Result;
import com.zfc.eldercare.core.dto.HealthRecordDTO;
import com.zfc.eldercare.core.service.HealthRecordService;
import com.zfc.eldercare.core.vo.HealthRecordVO;
import com.zfc.eldercare.core.vo.HealthTrendVO;
import com.zfc.eldercare.core.vo.PageVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 会员端健康记录接口（/api/member/health，详细设计文档 7.2）。
 */
@RestController
@RequestMapping("/api/member/health")
@RequiredArgsConstructor
public class HealthController {

    private final HealthRecordService healthRecordService;

    /** 录入健康数据（自动计算 BMI，超标触发健康提醒） */
    @PostMapping
    public Result<Long> record(@AuthenticationPrincipal Long userId,
                               @Valid @RequestBody HealthRecordDTO dto) {
        return Result.success("录入成功", healthRecordService.record(userId, dto));
    }

    /** 历史记录分页查询 */
    @GetMapping("/history")
    public Result<PageVO<HealthRecordVO>> history(@AuthenticationPrincipal Long userId,
                                                  @RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "10") int size) {
        return Result.success(healthRecordService.pageByUser(userId, page, size));
    }

    /** 近 6 个月健康趋势分析 */
    @GetMapping("/trend")
    public Result<HealthTrendVO> trend(@AuthenticationPrincipal Long userId) {
        return Result.success(healthRecordService.trend(userId, null));
    }
}
