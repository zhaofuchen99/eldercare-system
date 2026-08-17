package com.zfc.eldercare.core.service.impl;

import com.zfc.eldercare.core.mapper.ActivityRegistrationMapper;
import com.zfc.eldercare.core.mapper.AppointmentMapper;
import com.zfc.eldercare.core.mapper.UserMapper;
import com.zfc.eldercare.core.service.DashboardService;
import com.zfc.eldercare.core.vo.DashboardVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 仪表盘服务实现（详细设计文档 5.9）。
 * 单机项目数据量小，各指标直接 COUNT 实时统计即可，无需预聚合/缓存。
 */
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final UserMapper userMapper;
    private final AppointmentMapper appointmentMapper;
    private final ActivityRegistrationMapper activityRegistrationMapper;

    @Override
    public DashboardVO overview() {
        return new DashboardVO(
                userMapper.countMembers(),
                userMapper.countTodayNewMembers(),
                appointmentMapper.countToday(),
                activityRegistrationMapper.countToday(),
                appointmentMapper.countPending());
    }
}
