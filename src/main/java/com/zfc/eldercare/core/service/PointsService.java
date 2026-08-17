package com.zfc.eldercare.core.service;

/**
 * 积分服务（文档 5.8 / 6.3.18）。
 * 当前先提供注册、评测完成两个获得类入口；消费/FIFO/过期等后续积分模块扩展。
 */
public interface PointsService {

    /** 注册赠送积分：从 sys_config 读注册赠送值，原子增加 user.points 并写获得流水（1 年有效） */
    void registerBonus(Long userId);

    /** 完成健康评测赠送积分（默认 20，sys_config 可配） */
    void assessmentBonus(Long userId);
}
