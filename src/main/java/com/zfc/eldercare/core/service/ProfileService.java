package com.zfc.eldercare.core.service;

import com.zfc.eldercare.core.dto.ProfileUpdateDTO;
import com.zfc.eldercare.core.vo.ProfileVO;

/**
 * 个人中心服务（会员端，需求 4.1.8）。
 */
public interface ProfileService {

    /** 获取个人信息 */
    ProfileVO getProfile(Long userId);

    /** 更新个人信息（仅更新传入字段） */
    void updateProfile(Long userId, ProfileUpdateDTO dto);
}
