package com.zfc.eldercare.api.controller;

import com.zfc.eldercare.core.common.Result;
import com.zfc.eldercare.core.service.ActivityService;
import com.zfc.eldercare.core.vo.ActivityVO;
import com.zfc.eldercare.core.vo.CheckinStatusVO;
import com.zfc.eldercare.core.vo.MyActivityVO;
import com.zfc.eldercare.core.vo.PageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 会员端社区活动接口（/api/member/activity，详细设计文档 5.6 / 7.2）。
 */
@RestController
@RequestMapping("/api/member/activity")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    /** 活动列表（已发布，按开始时间倒序） */
    @GetMapping
    public Result<List<ActivityVO>> activities() {
        return Result.success(activityService.publishedActivities());
    }

    /** 活动详情 */
    @GetMapping("/{id}")
    public Result<ActivityVO> detail(@PathVariable Long id) {
        return Result.success(activityService.activityDetail(id));
    }

    /** 活动报名（校验报名期 + 名额并发控制） */
    @PostMapping("/{id}/register")
    public Result<Void> register(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        activityService.register(userId, id);
        return Result.success("报名成功", null);
    }

    /** 我的活动分页 */
    @GetMapping("/mine")
    public Result<PageVO<MyActivityVO>> mine(@AuthenticationPrincipal Long userId,
                                             @RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "10") int size) {
        return Result.success(activityService.myActivities(userId, page, size));
    }

    /** 当前用户在某活动的报名/签到状态 */
    @GetMapping("/{id}/checkin-status")
    public Result<CheckinStatusVO> checkinStatus(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        return Result.success(activityService.checkinStatus(userId, id));
    }

    /** 活动签到（校验已报名 + 进行中，签到赠送 50 积分） */
    @PostMapping("/{id}/checkin")
    public Result<Void> checkin(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        activityService.checkin(userId, id);
        return Result.success("签到成功", null);
    }
}
