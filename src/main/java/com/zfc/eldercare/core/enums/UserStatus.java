package com.zfc.eldercare.core.enums;

import lombok.Getter;

/**
 * 用户状态（对应 user.status）。
 */
@Getter
public enum UserStatus {

    ENABLED("启用"),
    DISABLED("禁用");

    private final String desc;

    UserStatus(String desc) {
        this.desc = desc;
    }
}
