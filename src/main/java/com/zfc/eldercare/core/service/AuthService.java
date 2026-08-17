package com.zfc.eldercare.core.service;

import com.zfc.eldercare.core.dto.ChangePasswordDTO;
import com.zfc.eldercare.core.dto.ForgotPasswordDTO;
import com.zfc.eldercare.core.dto.LoginDTO;
import com.zfc.eldercare.core.dto.RefreshTokenDTO;
import com.zfc.eldercare.core.dto.RegisterDTO;
import com.zfc.eldercare.core.vo.LoginVO;
import com.zfc.eldercare.core.vo.TokenVO;

/**
 * 认证授权服务（详细设计文档 5.1）。
 */
public interface AuthService {

    /** 注册（短信验证 + 赠送初始积分） */
    LoginVO register(RegisterDTO dto);

    /** 登录：校验密码/状态，签发双 Token */
    LoginVO login(LoginDTO dto);

    /** 刷新 Access Token */
    TokenVO refresh(RefreshTokenDTO dto);

    /** 登出：Access Token 进黑名单，Refresh Token 物理删除 */
    void logout(String authorization, String refreshToken);

    /** 密码找回：短信验证后重置密码，强制所有设备下线 */
    void forgotPassword(ForgotPasswordDTO dto);

    /** 修改密码（登录状态）：校验原密码后重置，强制所有设备下线 */
    void changePassword(Long userId, ChangePasswordDTO dto);
}
