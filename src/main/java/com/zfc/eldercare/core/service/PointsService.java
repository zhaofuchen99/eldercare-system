package com.zfc.eldercare.core.service;

/**
 * 积分服务（文档 5.8 / 6.3.18）。
 * 当前先提供认证注册所需的入口，消费/过期/FIFO 等后续积分模块扩展。
 */
public interface PointsService {

    /** 注册赠送积分：从 sys_config 读注册赠送值，原子增加 user.points 并写获得流水（1 年有效） */
    void registerBonus(Long userId);
}
