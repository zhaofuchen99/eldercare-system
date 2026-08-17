package com.zfc.eldercare.core.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.zfc.eldercare.core.entity.User;
import com.zfc.eldercare.core.exception.BusinessException;
import com.zfc.eldercare.core.mapper.UserMapper;
import com.zfc.eldercare.core.service.AuthService;
import com.zfc.eldercare.core.service.MemberService;
import com.zfc.eldercare.core.service.PointsService;
import com.zfc.eldercare.core.vo.MemberVO;
import com.zfc.eldercare.core.vo.PageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * 会员管理服务实现（详细设计文档 5.10）。
 * 重置密码：BCrypt 加密更新 + 强制该会员所有设备下线（删除 Refresh Token + 用户级黑名单，文档 5.10 步骤 4）。
 */
@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private static final String STATUS_ENABLED = "ENABLED";
    private static final String STATUS_DISABLED = "DISABLED";

    private final UserMapper userMapper;
    private final PointsService pointsService;
    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public PageVO<MemberVO> memberPage(String keyword, String status, String memberLevel, int page, int size) {
        PageHelper.startPage(page, size);
        List<User> list = userMapper.selectMemberPage(keyword, status, memberLevel);
        PageInfo<User> pageInfo = new PageInfo<>(list);
        List<MemberVO> voList = list.stream().map(MemberVO::from).toList();
        return new PageVO<>(pageInfo.getPageNum(), pageInfo.getPageSize(),
                pageInfo.getTotal(), pageInfo.getPages(), voList);
    }

    @Override
    public MemberVO memberDetail(Long id) {
        return MemberVO.from(requireMember(id));
    }

    @Override
    public void enable(Long id) {
        requireMember(id);
        userMapper.updateStatus(id, STATUS_ENABLED);
    }

    @Override
    public void disable(Long id) {
        requireMember(id);
        userMapper.updateStatus(id, STATUS_DISABLED);
    }

    @Override
    public void updateLevel(Long id, String memberLevel) {
        requireMember(id);
        userMapper.updateMemberLevel(id, memberLevel);
    }

    @Override
    @Transactional
    public int adjustPoints(Long id, int delta) {
        requireMember(id);
        return pointsService.adjustPoints(id, delta);
    }

    @Override
    @Transactional
    public String resetPassword(Long id, String password) {
        requireMember(id);
        String newPassword = (password == null || password.isBlank()) ? generatePassword() : password;
        userMapper.updatePassword(id, passwordEncoder.encode(newPassword));
        // 将该会员所有已发放 Token 加入黑名单，原登录态失效（文档 5.10 步骤 4）
        authService.forceLogoutUser(id);
        return newPassword;
    }

    // ========== 私有辅助 ==========

    private User requireMember(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "会员不存在");
        }
        return user;
    }

    /** 系统生成初始密码：10 位，至少含一个字母和一个数字（满足密码复杂度规则） */
    private String generatePassword() {
        String upper = "ABCDEFGHJKLMNPQRSTUVWXYZ";
        String lower = "abcdefghijkmnopqrstuvwxyz";
        String digits = "23456789";
        Random random = new Random();
        List<Character> chars = new ArrayList<>();
        chars.add(upper.charAt(random.nextInt(upper.length())));
        chars.add(digits.charAt(random.nextInt(digits.length())));
        String all = upper + lower + digits;
        for (int i = 0; i < 8; i++) {
            chars.add(all.charAt(random.nextInt(all.length())));
        }
        Collections.shuffle(chars, random);
        return chars.stream().map(String::valueOf).collect(Collectors.joining());
    }
}
