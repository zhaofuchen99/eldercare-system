package com.zfc.eldercare.core.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 短信验证码实体（对应 sms_code 表，used 字段代替逻辑删除）。
 */
@Data
public class SmsCode {

    private Long id;

    private String phone;

    private String code;

    private LocalDateTime expireTime;

    private Integer used;

    private LocalDateTime createTime;
}
