package com.zfc.eldercare.core.mapper;

import com.zfc.eldercare.core.entity.SysConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 系统配置表 Mapper（文档 6.3.17）。
 */
@Mapper
public interface SysConfigMapper {

    /** 按配置键取配置值 */
    String selectValueByKey(@Param("key") String configKey);

    /** 按配置键取完整配置（管理端获取/校验用） */
    SysConfig selectByKey(@Param("key") String configKey);

    /** 配置项列表（管理端） */
    List<SysConfig> selectAll();

    /** 更新配置值 */
    int updateValue(@Param("key") String configKey, @Param("value") String configValue);
}
