package com.zfc.eldercare.core.mapper;

import com.zfc.eldercare.core.entity.AppointmentSlot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 预约时段表 Mapper（文档 6.3.9）。
 */
@Mapper
public interface AppointmentSlotMapper {

    int insert(AppointmentSlot slot);

    /** 批量生成时段（管理端） */
    int insertBatch(@Param("list") List<AppointmentSlot> slots);

    AppointmentSlot selectById(@Param("id") Long id);

    /** 批量查询（VO 组装用） */
    List<AppointmentSlot> selectByIds(@Param("ids") List<Long> ids);

    /** 时段查询（管理端，可按套餐/日期过滤，均可不填） */
    List<AppointmentSlot> selectByPackage(@Param("packageId") Long packageId, @Param("date") LocalDate date);

    /** 可预约时段查询（会员端，未来日期且可预约） */
    List<AppointmentSlot> selectAvailable(@Param("packageId") Long packageId, @Param("date") LocalDate date);

    /** 原子增加预约人数并满员置位：仅 current_count < max_count 时成功（文档 9.3 并发控制） */
    int incrementCount(@Param("id") Long id);

    /** 原子减少预约人数（取消退还，FULL 变回 AVAILABLE） */
    int decrementCount(@Param("id") Long id);

    int updateStatus(@Param("id") Long id, @Param("status") String status);

    /** 逻辑删除 */
    int delete(@Param("id") Long id);
}
