package com.zfc.eldercare.core.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 系统配置表 Mapper。
 */
@Mapper
public interface SysConfigMapper {

    /** 按配置键取配置值 */
    String selectValueByKey(@Param("key") String configKey);
}
