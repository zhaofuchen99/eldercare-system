package com.zfc.eldercare.api.controller;

import com.zfc.eldercare.core.common.Result;
import com.zfc.eldercare.core.dto.SendSmsCodeDTO;
import com.zfc.eldercare.core.service.SmsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 短信接口（/api/sms，详细设计文档 7.2）。
 */
@RestController
@RequestMapping("/api/sms")
@RequiredArgsConstructor
public class SmsController {

    private final SmsService smsService;

    @PostMapping("/code")
    public Result<Void> sendCode(@Valid @RequestBody SendSmsCodeDTO dto) {
        smsService.sendCode(dto.phone());
        return Result.success("验证码已发送", null);
    }
}
