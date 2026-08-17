package com.zfc.eldercare.core.service;

import com.zfc.eldercare.core.dto.ActivityDTO;
import com.zfc.eldercare.core.vo.ActivityRegistrationVO;
import com.zfc.eldercare.core.vo.ActivityVO;
import com.zfc.eldercare.core.vo.CheckinStatusVO;
import com.zfc.eldercare.core.vo.MyActivityVO;
import com.zfc.eldercare.core.vo.PageVO;

import java.util.List;

/**
 * 社区活动服务（详细设计文档 5.6）。
 */
public interface ActivityService {

    // ========== 会员端 ==========

    /** 已发布活动列表（非草稿，按开始时间倒序） */
    List<ActivityVO> publishedActivities();

    /** 活动详情 */
    ActivityVO activityDetail(Long id);

    /** 活动报名（校验报名期 + 重复报名 + 名额并发控制） */
    void register(Long userId, Long activityId);

    /** 我的活动分页 */
    PageVO<MyActivityVO> myActivities(Long userId, int page, int size);

    /** 当前用户在某活动的报名/签到状态 */
    CheckinStatusVO checkinStatus(Long userId, Long activityId);

    /** 活动签到（校验已报名 + 进行中，防重复签到，签到赠送积分） */
    void checkin(Long userId, Long activityId);

    // ========== 管理端 ==========

    /** 创建活动（默认草稿） */
    Long createActivity(ActivityDTO dto);

    /** 编辑活动（字段缺省不更新） */
    void updateActivity(Long id, ActivityDTO dto);

    /** 删除活动（逻辑删除） */
    void deleteActivity(Long id);

    /** 活动分页 */
    PageVO<ActivityVO> activityPage(int page, int size);

    /** 活动详情（管理端） */
    ActivityVO adminActivityDetail(Long id);

    /** 活动报名列表（含报名人信息与签到情况） */
    List<ActivityRegistrationVO> registrations(Long activityId);

    /** 推送次日开始活动提醒短信（文档 5.12 定时任务） */
    void sendTomorrowReminders();
}
