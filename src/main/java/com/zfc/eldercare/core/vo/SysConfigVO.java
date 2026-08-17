package com.zfc.eldercare.core.vo;

import com.zfc.eldercare.core.entity.SysConfig;

import java.time.LocalDateTime;

/**
 * 系统配置 VO（管理端，文档 5.11）。
 */
public record SysConfigVO(
        Long id,
        String configKey,
        String configValue,
        String description,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
    public static SysConfigVO from(SysConfig c) {
        return new SysConfigVO(c.getId(), c.getConfigKey(), c.getConfigValue(),
                c.getDescription(), c.getCreateTime(), c.getUpdateTime());
    }
}
