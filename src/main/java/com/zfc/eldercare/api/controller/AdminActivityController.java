package com.zfc.eldercare.api.controller;

import com.zfc.eldercare.core.common.Result;
import com.zfc.eldercare.core.dto.ActivityDTO;
import com.zfc.eldercare.core.service.ActivityService;
import com.zfc.eldercare.core.vo.ActivityRegistrationVO;
import com.zfc.eldercare.core.vo.ActivityVO;
import com.zfc.eldercare.core.vo.PageVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端活动管理接口（/api/admin/activity，详细设计文档 5.6 / 7.2）。
 */
@RestController
@RequestMapping("/api/admin/activity")
@RequiredArgsConstructor
public class AdminActivityController {

    private final ActivityService activityService;

    /** 创建活动（默认草稿） */
    @PostMapping
    public Result<Long> create(@Valid @RequestBody ActivityDTO dto) {
        return Result.success("创建成功", activityService.createActivity(dto));
    }

    /** 编辑活动（字段缺省不更新；改 status 可发布/结束） */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ActivityDTO dto) {
        activityService.updateActivity(id, dto);
        return Result.success("更新成功", null);
    }

    /** 删除活动（逻辑删除） */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        activityService.deleteActivity(id);
        return Result.success("删除成功", null);
    }

    /** 活动分页 */
    @GetMapping("/page")
    public Result<PageVO<ActivityVO>> page(@RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "10") int size) {
        return Result.success(activityService.activityPage(page, size));
    }

    /** 活动详情（管理端） */
    @GetMapping("/{id}")
    public Result<ActivityVO> detail(@PathVariable Long id) {
        return Result.success(activityService.adminActivityDetail(id));
    }

    /** 活动报名列表（含报名人信息与签到情况） */
    @GetMapping("/{id}/registrations")
    public Result<List<ActivityRegistrationVO>> registrations(@PathVariable Long id) {
        return Result.success(activityService.registrations(id));
    }
}
