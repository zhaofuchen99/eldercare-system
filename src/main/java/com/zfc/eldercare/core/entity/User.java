package com.zfc.eldercare.core.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户实体（对应 user 表）。
 * 注意：密码字段不会通过实体直接返回给前端（登录返回用 VO 映射）。
 */
@Data
public class User {

    private Long id;

    private String phone;

    private String password;

    private String realName;

    private String gender;

    private LocalDate birthDate;

    private BigDecimal height;

    private String avatar;

    private String emergencyContact;

    private String memberLevel;

    private Integer points;

    private String status;

    private String role;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer deleted;
}
