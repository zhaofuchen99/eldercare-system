package com.zfc.eldercare.core.mapper;

import com.zfc.eldercare.core.entity.Appointment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 预约表 Mapper（文档 6.3.10）。
 */
@Mapper
public interface AppointmentMapper {

    int insert(Appointment appointment);

    Appointment selectById(@Param("id") Long id);

    /** 我的预约分页（会员端），配合 PageHelper */
    List<Appointment> selectPageByUserId(@Param("userId") Long userId);

    /** 预约分页（管理端，可按状态/用户/套餐/时段日期过滤），配合 PageHelper */
    List<Appointment> selectPage(@Param("status") String status,
                                 @Param("userId") Long userId,
                                 @Param("packageId") Long packageId,
                                 @Param("appointDate") LocalDate appointDate);

    int updateStatus(@Param("id") Long id, @Param("status") String status);

    /** 上传体检报告并置为已完成 */
    int uploadReport(@Param("id") Long id, @Param("reportUrl") String reportUrl,
                     @Param("originalFilename") String originalFilename, @Param("uploadAdminId") Long uploadAdminId);

    /** 取消预约（仅状态更新；积分/名额退还在服务层处理） */
    int cancelById(@Param("id") Long id);

    /** 今日预约数（文档 5.9 仪表盘） */
    long countToday();

    /** 待完成预约数（状态为待确认/已确认，即 PENDING/CONFIRMED，文档 5.9） */
    long countPending();

    /** 归档过期历史预约（保留 2 年，逻辑删除，文档 5.12） */
    int archiveExpired(@Param("beforeTime") LocalDateTime beforeTime);

    /** 次日预约提醒：已确认且时段日期为指定日期的预约（文档 5.12） */
    List<Appointment> selectConfirmedOnDate(@Param("appointDate") LocalDate appointDate);
}
