package com.zfc.eldercare.core.service.impl;

import com.zfc.eldercare.core.dto.ProfileUpdateDTO;
import com.zfc.eldercare.core.entity.User;
import com.zfc.eldercare.core.exception.BusinessException;
import com.zfc.eldercare.core.mapper.UserMapper;
import com.zfc.eldercare.core.service.ProfileService;
import com.zfc.eldercare.core.vo.ProfileVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 个人中心服务实现（会员端，需求 4.1.8）。
 * 更新仅写入非 null 字段（部分更新），不影响其它字段。
 */
@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final UserMapper userMapper;

    @Override
    public ProfileVO getProfile(Long userId) {
        return ProfileVO.from(requireUser(userId));
    }

    @Override
    public void updateProfile(Long userId, ProfileUpdateDTO dto) {
        requireUser(userId);
        User update = new User();
        update.setId(userId);
        update.setRealName(dto.realName());
        update.setGender(dto.gender());
        update.setBirthDate(dto.birthDate());
        update.setHeight(dto.height());
        update.setAvatar(dto.avatar());
        update.setEmergencyContact(dto.emergencyContact());
        userMapper.updateProfile(update);
    }

    private User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return user;
    }
}
