package com.zfc.eldercare.core.vo;

import com.zfc.eldercare.core.entity.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 会员管理 VO（管理端，文档 5.10，基本信息 + 积分，不含密码）。
 */
public record MemberVO(
        Long id,
        String phone,
        String realName,
        String gender,
        LocalDate birthDate,
        BigDecimal height,
        String avatar,
        String emergencyContact,
        String memberLevel,
        Integer points,
        String status,
        LocalDateTime createTime
) {
    public static MemberVO from(User u) {
        return new MemberVO(u.getId(), u.getPhone(), u.getRealName(), u.getGender(),
                u.getBirthDate(), u.getHeight(), u.getAvatar(), u.getEmergencyContact(),
                u.getMemberLevel(), u.getPoints(), u.getStatus(), u.getCreateTime());
    }
}
