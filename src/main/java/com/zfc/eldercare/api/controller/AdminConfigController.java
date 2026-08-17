package com.zfc.eldercare.api.controller;

import com.zfc.eldercare.core.common.Result;
import com.zfc.eldercare.core.dto.SysConfigUpdateDTO;
import com.zfc.eldercare.core.service.ConfigService;
import com.zfc.eldercare.core.vo.SysConfigVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端系统配置接口（/api/admin/config，详细设计文档 5.11 / 7.2）。
 * 配置项列表、获取、更新。
 */
@RestController
@RequestMapping("/api/admin/config")
@RequiredArgsConstructor
public class AdminConfigController {

    private final ConfigService configService;

    /** 配置项列表 */
    @GetMapping
    public Result<List<SysConfigVO>> list() {
        return Result.success(configService.list());
    }

    /** 获取单个配置 */
    @GetMapping("/{key}")
    public Result<SysConfigVO> get(@PathVariable String key) {
        return Result.success(configService.get(key));
    }

    /** 更新配置值 */
    @PutMapping("/{key}")
    public Result<Void> update(@PathVariable String key, @Valid @RequestBody SysConfigUpdateDTO dto) {
        configService.update(key, dto.configValue());
        return Result.success("更新成功", null);
    }
}
