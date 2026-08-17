package com.zfc.eldercare.core.service;

import com.zfc.eldercare.core.vo.SysConfigVO;

import java.util.List;

/**
 * 系统配置服务（详细设计文档 5.11，管理端）。
 */
public interface ConfigService {

    /** 配置项列表 */
    List<SysConfigVO> list();

    /** 获取单个配置 */
    SysConfigVO get(String key);

    /** 更新配置值 */
    void update(String key, String configValue);
}
