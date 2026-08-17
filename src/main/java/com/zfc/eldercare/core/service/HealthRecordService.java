package com.zfc.eldercare.core.service;

import com.zfc.eldercare.core.dto.HealthRecordDTO;
import com.zfc.eldercare.core.vo.HealthRecordVO;
import com.zfc.eldercare.core.vo.HealthTrendVO;
import com.zfc.eldercare.core.vo.PageVO;

/**
 * 健康记录服务（详细设计文档 5.2）。
 */
public interface HealthRecordService {

    /**
     * 录入健康数据：自动计算 BMI，超标时触发健康提醒（同日去重）。
     *
     * @return 健康记录 ID
     */
    Long record(Long userId, HealthRecordDTO dto);

    /** 分页查询某用户健康记录（会员查自己/管理端查指定会员） */
    PageVO<HealthRecordVO> pageByUser(Long userId, int page, int size);

    /**
     * 健康趋势分析：近 6 个月按月聚合（平均值/最大值/最小值）。
     *
     * @param indicator 指标编码，null/空白时返回全部指标
     */
    HealthTrendVO trend(Long userId, String indicator);
}
