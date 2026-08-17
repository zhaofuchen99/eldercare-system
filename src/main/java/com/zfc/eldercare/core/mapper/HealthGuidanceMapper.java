package com.zfc.eldercare.core.mapper;

import com.zfc.eldercare.core.entity.HealthGuidance;
import org.apache.ibatis.annotations.Param;

/**
 * 健康指导 Mapper（health_guidance 表）。
 */
public interface HealthGuidanceMapper {

    int insert(HealthGuidance guidance);

    /** 查询某用户某指标当天的指导记录（同日去重用，返回 null 表示当日未触发过） */
    HealthGuidance selectTodayByIndicator(@Param("userId") Long userId, @Param("indicator") String indicator);
}
