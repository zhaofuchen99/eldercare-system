package com.zfc.eldercare.core.enums;

import lombok.Getter;

/**
 * 用户角色（对应 user.role 冗余字段，与 user_role 关联表保持一致）。
 */
@Getter
public enum Role {

    MEMBER("会员"),
    ADMIN("管理员");

    private final String desc;

    Role(String desc) {
        this.desc = desc;
    }
}
