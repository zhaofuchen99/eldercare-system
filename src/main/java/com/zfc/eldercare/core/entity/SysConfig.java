package com.zfc.eldercare.core.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统配置实体（对应 sys_config 表）。
 */
@Data
public class SysConfig {

    private Long id;

    private String configKey;

    private String configValue;

    private String description;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer deleted;
}
