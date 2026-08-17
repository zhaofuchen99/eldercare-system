package com.zfc.eldercare.api.controller;

import com.zfc.eldercare.core.common.Result;
import com.zfc.eldercare.core.service.PointsService;
import com.zfc.eldercare.core.vo.PageVO;
import com.zfc.eldercare.core.vo.PointTransactionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 会员端积分接口（/api/member/points，详细设计文档 5.8 / 7.2）。
 * 积分明细列表（分页）：获得/消费/过期等流水，按时间倒序。
 */
@RestController
@RequestMapping("/api/member/points")
@RequiredArgsConstructor
public class PointsController {

    private final PointsService pointsService;

    /** 积分明细（分页） */
    @GetMapping
    public Result<PageVO<PointTransactionVO>> page(@AuthenticationPrincipal Long userId,
                                                   @RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "10") int size) {
        return Result.success(pointsService.pointPage(userId, page, size));
    }
}
