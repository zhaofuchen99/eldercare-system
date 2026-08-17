package com.zfc.eldercare.core.service.impl;

import com.zfc.eldercare.core.entity.SysConfig;
import com.zfc.eldercare.core.exception.BusinessException;
import com.zfc.eldercare.core.mapper.SysConfigMapper;
import com.zfc.eldercare.core.service.ConfigService;
import com.zfc.eldercare.core.vo.SysConfigVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 系统配置服务实现（详细设计文档 5.11）。
 */
@Service
@RequiredArgsConstructor
public class ConfigServiceImpl implements ConfigService {

    private final SysConfigMapper sysConfigMapper;

    @Override
    public List<SysConfigVO> list() {
        return sysConfigMapper.selectAll().stream().map(SysConfigVO::from).toList();
    }

    @Override
    public SysConfigVO get(String key) {
        return SysConfigVO.from(requireConfig(key));
    }

    @Override
    public void update(String key, String configValue) {
        requireConfig(key);
        sysConfigMapper.updateValue(key, configValue);
    }

    private SysConfig requireConfig(String key) {
        SysConfig config = sysConfigMapper.selectByKey(key);
        if (config == null) {
            throw new BusinessException(404, "配置项不存在");
        }
        return config;
    }
}
