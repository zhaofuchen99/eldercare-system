package com.zfc.eldercare.core.enums;

import lombok.Getter;

/**
 * 会员等级（对应 user.member_level，数据库存英文编码，前端映射中文展示）。
 */
@Getter
public enum MemberLevel {

    NORMAL("普通会员"),
    SILVER("白银会员"),
    GOLD("黄金会员"),
    PLATINUM("铂金会员"),
    DIAMOND("钻石会员");

    private final String desc;

    MemberLevel(String desc) {
        this.desc = desc;
    }
}
