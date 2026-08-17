package com.zfc.eldercare.core.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.zfc.eldercare.core.dto.ActivityDTO;
import com.zfc.eldercare.core.entity.ActivityRegistration;
import com.zfc.eldercare.core.entity.CommunityActivity;
import com.zfc.eldercare.core.entity.User;
import com.zfc.eldercare.core.exception.BusinessException;
import com.zfc.eldercare.core.mapper.ActivityRegistrationMapper;
import com.zfc.eldercare.core.mapper.CommunityActivityMapper;
import com.zfc.eldercare.core.mapper.UserMapper;
import com.zfc.eldercare.core.service.ActivityService;
import com.zfc.eldercare.core.service.PointsService;
import com.zfc.eldercare.core.vo.ActivityRegistrationVO;
import com.zfc.eldercare.core.vo.ActivityVO;
import com.zfc.eldercare.core.vo.CheckinStatusVO;
import com.zfc.eldercare.core.vo.MyActivityVO;
import com.zfc.eldercare.core.vo.PageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 社区活动服务实现（详细设计文档 5.6 / 9.3 并发控制 / 5.8 签到积分）。
 * 报名：报名期校验 + 重复报名校验 + 名额原子占用（行锁）；签到：已报名校验 + 活动时间校验 + 防重复 + 签到积分。
 */
@Service
@RequiredArgsConstructor
public class ActivityServiceImpl implements ActivityService {

    private static final String STATUS_ENABLED = "ENABLED";
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_REGISTRATING = "REGISTRATING";
    private static final String STATUS_ENDED = "ENDED";
    private static final String CHECKIN_NOT = "NOT_CHECKED_IN";

    private final CommunityActivityMapper communityActivityMapper;
    private final ActivityRegistrationMapper activityRegistrationMapper;
    private final UserMapper userMapper;
    private final PointsService pointsService;

    // ========== 会员端 ==========

    @Override
    public List<ActivityVO> publishedActivities() {
        return communityActivityMapper.selectPublished().stream()
                .map(ActivityVO::from)
                .toList();
    }

    @Override
    public ActivityVO activityDetail(Long id) {
        return ActivityVO.from(requireActivity(id));
    }

    @Override
    @Transactional
    public void register(Long userId, Long activityId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if (!STATUS_ENABLED.equals(user.getStatus())) {
            throw new BusinessException(403, "账号已被禁用，无法报名");
        }

        CommunityActivity activity = requireActivity(activityId);
        if (!STATUS_REGISTRATING.equals(activity.getStatus())) {
            throw new BusinessException(409, "该活动不在报名期");
        }
        // 报名时间窗口
        LocalDateTime now = LocalDateTime.now();
        if (activity.getRegistrationStartTime() != null && now.isBefore(activity.getRegistrationStartTime())) {
            throw new BusinessException(409, "报名尚未开始");
        }
        if (activity.getRegistrationEndTime() != null && now.isAfter(activity.getRegistrationEndTime())) {
            throw new BusinessException(409, "报名已结束");
        }

        // 重复报名校验（唯一键 uk_user_activity 兜底）
        if (activityRegistrationMapper.selectByUserAndActivity(userId, activityId) != null) {
            throw new BusinessException(409, "您已报名该活动");
        }

        // 原子占用名额（未达上限才成功，行锁防并发超员；任一后续失败整体回滚）
        int cnt = communityActivityMapper.incrementParticipants(activityId);
        if (cnt == 0) {
            throw new BusinessException(409, "活动名额已满");
        }

        ActivityRegistration registration = new ActivityRegistration();
        registration.setUserId(userId);
        registration.setActivityId(activityId);
        registration.setCheckInStatus(CHECKIN_NOT);
        activityRegistrationMapper.insert(registration);
    }

    @Override
    public PageVO<MyActivityVO> myActivities(Long userId, int page, int size) {
        PageHelper.startPage(page, size);
        List<ActivityRegistration> list = activityRegistrationMapper.selectPageByUserId(userId);
        PageInfo<ActivityRegistration> pageInfo = new PageInfo<>(list);
        List<MyActivityVO> voList = toMyActivityVO(list);
        return new PageVO<>(pageInfo.getPageNum(), pageInfo.getPageSize(),
                pageInfo.getTotal(), pageInfo.getPages(), voList);
    }

    @Override
    public CheckinStatusVO checkinStatus(Long userId, Long activityId) {
        requireActivity(activityId);
        ActivityRegistration registration = activityRegistrationMapper.selectByUserAndActivity(userId, activityId);
        if (registration == null) {
            return new CheckinStatusVO(activityId, false, null, null);
        }
        return new CheckinStatusVO(activityId, true, registration.getCheckInStatus(), registration.getCheckInTime());
    }

    @Override
    @Transactional
    public void checkin(Long userId, Long activityId) {
        CommunityActivity activity = requireActivity(activityId);
        if (STATUS_DRAFT.equals(activity.getStatus())) {
            throw new BusinessException(409, "活动尚未发布");
        }
        if (STATUS_ENDED.equals(activity.getStatus())) {
            throw new BusinessException(409, "活动已结束");
        }

        // 校验已报名（文档 5.6 签到流程步骤 1）
        ActivityRegistration registration = activityRegistrationMapper.selectByUserAndActivity(userId, activityId);
        if (registration == null) {
            throw new BusinessException(409, "请先报名该活动");
        }

        // 校验活动进行中（步骤 2，按活动时间窗口判断）
        LocalDateTime now = LocalDateTime.now();
        if (activity.getActivityStartTime() == null || now.isBefore(activity.getActivityStartTime())) {
            throw new BusinessException(409, "活动尚未开始");
        }
        if (activity.getActivityEndTime() != null && now.isAfter(activity.getActivityEndTime())) {
            throw new BusinessException(409, "活动已结束");
        }

        // 原子更新签到状态（步骤 3，防重复签到）
        int rows = activityRegistrationMapper.checkIn(registration.getId());
        if (rows == 0) {
            throw new BusinessException(409, "您已签到过该活动");
        }

        // 原子增加签到积分 50（步骤 4，sys_config checkin_bonus_points 可配）
        pointsService.checkinBonus(userId);
    }

    // ========== 管理端 ==========

    @Override
    public Long createActivity(ActivityDTO dto) {
        if (!StringUtils.hasText(dto.title())) {
            throw new BusinessException("活动标题不能为空");
        }
        CommunityActivity activity = copyDto(dto);
        activity.setCurrentParticipants(0);
        activity.setStatus(dto.status() == null ? STATUS_DRAFT : dto.status());
        communityActivityMapper.insert(activity);
        return activity.getId();
    }

    @Override
    public void updateActivity(Long id, ActivityDTO dto) {
        if (communityActivityMapper.selectById(id) == null) {
            throw new BusinessException(404, "活动不存在");
        }
        CommunityActivity activity = copyDto(dto);
        activity.setId(id);
        activity.setStatus(dto.status());
        communityActivityMapper.update(activity);
    }

    /** 公共字段拷贝（不含 id / currentParticipants / status） */
    private CommunityActivity copyDto(ActivityDTO dto) {
        CommunityActivity activity = new CommunityActivity();
        activity.setTitle(dto.title());
        activity.setCoverUrl(dto.coverUrl());
        activity.setContent(dto.content());
        activity.setLocation(dto.location());
        activity.setRegistrationStartTime(dto.registrationStartTime());
        activity.setRegistrationEndTime(dto.registrationEndTime());
        activity.setActivityStartTime(dto.activityStartTime());
        activity.setActivityEndTime(dto.activityEndTime());
        activity.setMaxParticipants(dto.maxParticipants());
        return activity;
    }

    @Override
    public void deleteActivity(Long id) {
        if (communityActivityMapper.selectById(id) == null) {
            throw new BusinessException(404, "活动不存在");
        }
        communityActivityMapper.delete(id);
    }

    @Override
    public PageVO<ActivityVO> activityPage(int page, int size) {
        PageHelper.startPage(page, size);
        List<CommunityActivity> list = communityActivityMapper.selectPage();
        PageInfo<CommunityActivity> pageInfo = new PageInfo<>(list);
        List<ActivityVO> voList = list.stream().map(ActivityVO::from).toList();
        return new PageVO<>(pageInfo.getPageNum(), pageInfo.getPageSize(),
                pageInfo.getTotal(), pageInfo.getPages(), voList);
    }

    @Override
    public ActivityVO adminActivityDetail(Long id) {
        return ActivityVO.from(requireActivity(id));
    }

    @Override
    public List<ActivityRegistrationVO> registrations(Long activityId) {
        requireActivity(activityId);
        List<ActivityRegistration> list = activityRegistrationMapper.selectByActivityId(activityId);
        if (list.isEmpty()) {
            return List.of();
        }
        List<Long> userIds = list.stream().map(ActivityRegistration::getUserId).distinct().toList();
        Map<Long, User> userMap = userMapper.selectByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        return list.stream()
                .map(r -> {
                    User user = userMap.get(r.getUserId());
                    return ActivityRegistrationVO.from(r,
                            user == null ? null : user.getRealName(),
                            user == null ? null : user.getPhone());
                })
                .toList();
    }

    // ========== 私有辅助 ==========

    private CommunityActivity requireActivity(Long activityId) {
        CommunityActivity activity = communityActivityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException(404, "活动不存在");
        }
        return activity;
    }

    private List<MyActivityVO> toMyActivityVO(List<ActivityRegistration> list) {
        if (list.isEmpty()) {
            return List.of();
        }
        List<Long> activityIds = list.stream().map(ActivityRegistration::getActivityId).distinct().toList();
        Map<Long, CommunityActivity> activityMap = communityActivityMapper.selectByIds(activityIds).stream()
                .collect(Collectors.toMap(CommunityActivity::getId, Function.identity()));
        return list.stream()
                .map(r -> MyActivityVO.from(r, activityMap.get(r.getActivityId())))
                .toList();
    }
}
