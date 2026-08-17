package com.zfc.eldercare.core.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis 配置：扫描 Mapper 接口包。
 */
@Configuration
@MapperScan("com.zfc.eldercare.core.mapper")
public class MyBatisConfig {
}
