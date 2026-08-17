package com.zfc.eldercare.core.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 刷新令牌实体（对应 refresh_token 表，物理删除，无 deleted 字段）。
 */
@Data
public class RefreshToken {

    private Long id;

    private Long userId;

    private String token;

    private LocalDateTime expireTime;

    private LocalDateTime createTime;
}
