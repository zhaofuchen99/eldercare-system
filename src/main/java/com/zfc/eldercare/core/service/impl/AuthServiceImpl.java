package com.zfc.eldercare.core.service.impl;

import com.zfc.eldercare.core.dto.ChangePasswordDTO;
import com.zfc.eldercare.core.dto.ForgotPasswordDTO;
import com.zfc.eldercare.core.dto.LoginDTO;
import com.zfc.eldercare.core.dto.RefreshTokenDTO;
import com.zfc.eldercare.core.dto.RegisterDTO;
import com.zfc.eldercare.core.entity.RefreshToken;
import com.zfc.eldercare.core.entity.User;
import com.zfc.eldercare.core.enums.MemberLevel;
import com.zfc.eldercare.core.enums.Role;
import com.zfc.eldercare.core.enums.UserStatus;
import com.zfc.eldercare.core.exception.BusinessException;
import com.zfc.eldercare.core.mapper.RefreshTokenMapper;
import com.zfc.eldercare.core.mapper.UserMapper;
import com.zfc.eldercare.core.service.AuthService;
import com.zfc.eldercare.core.service.PointsService;
import com.zfc.eldercare.core.service.SmsService;
import com.zfc.eldercare.core.util.JwtUtil;
import com.zfc.eldercare.core.vo.LoginVO;
import com.zfc.eldercare.core.vo.TokenVO;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 认证授权服务实现（详细设计文档 5.1 各流程）。
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final RefreshTokenMapper refreshTokenMapper;
    private final SmsService smsService;
    private final PointsService pointsService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Value("${jwt.access-token-expire}")
    private long accessExpireSeconds;

    @Value("${jwt.refresh-token-expire}")
    private long refreshExpireSeconds;

    @Override
    @Transactional
    public LoginVO register(RegisterDTO dto) {
        // 1. 校验手机号未注册
        if (userMapper.selectByPhone(dto.phone()) != null) {
            throw new BusinessException(409, "手机号已注册");
        }
        // 2. 校验短信验证码
        smsService.verifyCode(dto.phone(), dto.code());
        // 3. BCrypt 加密创建用户
        User user = new User();
        user.setPhone(dto.phone());
        user.setPassword(passwordEncoder.encode(dto.password()));
        user.setMemberLevel(MemberLevel.NORMAL.name());
        user.setPoints(0);
        user.setStatus(UserStatus.ENABLED.name());
        user.setRole(Role.MEMBER.name());
        userMapper.insert(user);
        // 4. 赠送初始积分（写流水）
        pointsService.registerBonus(user.getId());
        // 5. 签发 Token 返回
        return doLogin(user);
    }

    @Override
    @Transactional
    public LoginVO login(LoginDTO dto) {
        User user = userMapper.selectByPhone(dto.phone());
        if (user == null || !passwordEncoder.matches(dto.password(), user.getPassword())) {
            throw new BusinessException("手机号或密码错误");
        }
        if (!UserStatus.ENABLED.name().equals(user.getStatus())) {
            throw new BusinessException("账号已被禁用，请联系管理员");
        }
        return doLogin(user);
    }

    @Override
    @Transactional
    public TokenVO refresh(RefreshTokenDTO dto) {
        RefreshToken rt = refreshTokenMapper.selectByToken(dto.refreshToken());
        if (rt == null || rt.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException(401, "Refresh Token 无效或已过期，请重新登录");
        }
        // 用户被强制下线（密码找回/重置）后，用户级黑名单生效则拒绝刷新
        if (jwtUtil.isUserBlacklisted(rt.getUserId())) {
            throw new BusinessException(401, "Refresh Token 已失效，请重新登录");
        }
        User user = userMapper.selectById(rt.getUserId());
        if (user == null || !UserStatus.ENABLED.name().equals(user.getStatus())) {
            throw new BusinessException(401, "用户不存在或已被禁用");
        }
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getRole());
        return new TokenVO(accessToken, rt.getToken(), "Bearer", accessExpireSeconds);
    }

    @Override
    public void logout(String authorization, String refreshToken) {
        // 1. Access Token 加入 Redis 黑名单（TTL 与剩余有效期一致）
        if (authorization != null && authorization.startsWith("Bearer ")) {
            Claims claims = jwtUtil.parseClaims(authorization.substring(7));
            if (claims != null) {
                long remainingMillis = claims.getExpiration().getTime() - System.currentTimeMillis();
                if (remainingMillis > 0) {
                    jwtUtil.addTokenToBlacklist(jwtUtil.getJti(claims), remainingMillis);
                }
            }
        }
        // 2. Refresh Token 从数据库物理删除（文档 6.3.2）
        if (refreshToken != null) {
            refreshTokenMapper.deleteByToken(refreshToken);
        }
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordDTO dto) {
        User user = userMapper.selectByPhone(dto.phone());
        if (user == null) {
            throw new BusinessException("该手机号未注册");
        }
        smsService.verifyCode(dto.phone(), dto.code());
        userMapper.updatePassword(user.getId(), passwordEncoder.encode(dto.newPassword()));
        // 强制所有设备下线
        forceLogout(user.getId());
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null || !passwordEncoder.matches(dto.oldPassword(), user.getPassword())) {
            throw new BusinessException("原密码错误");
        }
        userMapper.updatePassword(userId, passwordEncoder.encode(dto.newPassword()));
        forceLogout(userId);
    }

    /** 登录成功后签发双 Token，Refresh Token 入库，解除用户级黑名单 */
    private LoginVO doLogin(User user) {
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        RefreshToken rt = new RefreshToken();
        rt.setUserId(user.getId());
        rt.setToken(refreshToken);
        rt.setExpireTime(LocalDateTime.now().plusSeconds(refreshExpireSeconds));
        refreshTokenMapper.insert(rt);

        // 用户已重新认证，解除用户级黑名单（此前密码找回/重置可能屏蔽）
        jwtUtil.removeUserBlacklist(user.getId());

        return LoginVO.of(accessToken, refreshToken, accessExpireSeconds, user);
    }

    /** 强制该用户所有设备下线：删除全部 Refresh Token + 用户级黑名单（文档 5.1） */
    private void forceLogout(Long userId) {
        refreshTokenMapper.deleteByUserId(userId);
        jwtUtil.addUserToBlacklist(userId);
    }

    @Override
    @Transactional
    public void forceLogoutUser(Long userId) {
        forceLogout(userId);
    }
}
