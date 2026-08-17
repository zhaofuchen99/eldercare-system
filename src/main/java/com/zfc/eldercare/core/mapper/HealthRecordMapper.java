package com.zfc.eldercare.core.mapper;

import com.zfc.eldercare.core.entity.HealthRecord;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 健康记录 Mapper（health_record 表）。
 */
public interface HealthRecordMapper {

    int insert(HealthRecord record);

    /** 分页/列表查询某用户健康记录（倒序），配合 PageHelper 使用 */
    List<HealthRecord> selectByUserId(@Param("userId") Long userId);

    /** 查询某用户自指定时间起的全部健康记录（趋势分析用） */
    List<HealthRecord> selectSince(@Param("userId") Long userId, @Param("startTime") LocalDateTime startTime);
}
