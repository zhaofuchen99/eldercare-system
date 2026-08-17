package com.zfc.eldercare.api.controller;

import com.zfc.eldercare.core.common.Result;
import com.zfc.eldercare.core.dto.ChangePasswordDTO;
import com.zfc.eldercare.core.dto.ForgotPasswordDTO;
import com.zfc.eldercare.core.dto.LoginDTO;
import com.zfc.eldercare.core.dto.LogoutDTO;
import com.zfc.eldercare.core.dto.RefreshTokenDTO;
import com.zfc.eldercare.core.dto.RegisterDTO;
import com.zfc.eldercare.core.service.AuthService;
import com.zfc.eldercare.core.vo.LoginVO;
import com.zfc.eldercare.core.vo.TokenVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证授权接口（/api/auth，详细设计文档 7.2）。
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public Result<LoginVO> register(@Valid @RequestBody RegisterDTO dto) {
        return Result.success(authService.register(dto));
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success(authService.login(dto));
    }

    @PostMapping("/refresh")
    public Result<TokenVO> refresh(@Valid @RequestBody RefreshTokenDTO dto) {
        return Result.success(authService.refresh(dto));
    }

    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization,
                               @Valid @RequestBody LogoutDTO dto) {
        authService.logout(authorization, dto.refreshToken());
        return Result.success();
    }

    @PostMapping("/forgot-password")
    public Result<Void> forgotPassword(@Valid @RequestBody ForgotPasswordDTO dto) {
        authService.forgotPassword(dto);
        return Result.success();
    }

    @PostMapping("/change-password")
    public Result<Void> changePassword(@AuthenticationPrincipal Long userId,
                                       @Valid @RequestBody ChangePasswordDTO dto) {
        authService.changePassword(userId, dto);
        return Result.success();
    }
}
