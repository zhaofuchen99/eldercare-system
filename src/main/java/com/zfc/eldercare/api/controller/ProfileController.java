package com.zfc.eldercare.api.controller;

import com.zfc.eldercare.core.common.Result;
import com.zfc.eldercare.core.dto.ProfileUpdateDTO;
import com.zfc.eldercare.core.service.ProfileService;
import com.zfc.eldercare.core.vo.ProfileVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 会员端个人中心接口（/api/member/profile，需求 4.1.8 / 文档 7.2）。
 * 获取信息、更新信息；修改密码复用 /api/auth/change-password（1.7）。
 */
@RestController
@RequestMapping("/api/member/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    /** 获取个人信息 */
    @GetMapping
    public Result<ProfileVO> get(@AuthenticationPrincipal Long userId) {
        return Result.success(profileService.getProfile(userId));
    }

    /** 更新个人信息（仅更新传入字段） */
    @PutMapping
    public Result<Void> update(@AuthenticationPrincipal Long userId,
                               @Valid @RequestBody ProfileUpdateDTO dto) {
        profileService.updateProfile(userId, dto);
        return Result.success("更新成功", null);
    }
}
