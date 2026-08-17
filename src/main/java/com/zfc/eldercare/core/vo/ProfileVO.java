package com.zfc.eldercare.core.vo;

import com.zfc.eldercare.core.entity.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 个人中心 VO（会员端，需求 4.1.8，不含密码）。
 */
public record ProfileVO(
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
        String role,
        LocalDateTime createTime
) {
    public static ProfileVO from(User u) {
        return new ProfileVO(u.getId(), u.getPhone(), u.getRealName(), u.getGender(),
                u.getBirthDate(), u.getHeight(), u.getAvatar(), u.getEmergencyContact(),
                u.getMemberLevel(), u.getPoints(), u.getRole(), u.getCreateTime());
    }
}
