package com.zfc.eldercare.core.mapper;

import com.zfc.eldercare.core.entity.ActivityRegistration;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 活动报名表 Mapper（文档 6.3.12）。
 */
@Mapper
public interface ActivityRegistrationMapper {

    int insert(ActivityRegistration registration);

    /** 按用户+活动查报名（唯一键 uk_user_activity 防重复报名） */
    ActivityRegistration selectByUserAndActivity(@Param("userId") Long userId, @Param("activityId") Long activityId);

    /** 我的活动报名分页（会员端），配合 PageHelper */
    List<ActivityRegistration> selectPageByUserId(@Param("userId") Long userId);

    /** 活动报名列表（管理端，含签到情况） */
    List<ActivityRegistration> selectByActivityId(@Param("activityId") Long activityId);

    /** 原子签到：仅未签到成功，返回受影响行数（0=重复签到） */
    int checkIn(@Param("id") Long id);
}
