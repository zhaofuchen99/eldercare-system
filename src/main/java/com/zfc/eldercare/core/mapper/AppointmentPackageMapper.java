package com.zfc.eldercare.core.mapper;

import com.zfc.eldercare.core.entity.AppointmentPackage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 体检套餐表 Mapper（文档 6.3.8）。
 */
@Mapper
public interface AppointmentPackageMapper {

    int insert(AppointmentPackage pkg);

    AppointmentPackage selectById(@Param("id") Long id);

    /** 批量查询（VO 组装用） */
    List<AppointmentPackage> selectByIds(@Param("ids") List<Long> ids);

    /** 套餐分页（管理端），配合 PageHelper */
    List<AppointmentPackage> selectPage();

    /** 启用套餐列表（会员端） */
    List<AppointmentPackage> selectEnabled();

    int update(AppointmentPackage pkg);

    /** 逻辑删除 */
    int delete(@Param("id") Long id);
}
