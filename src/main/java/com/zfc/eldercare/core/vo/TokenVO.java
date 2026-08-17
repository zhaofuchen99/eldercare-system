package com.zfc.eldercare.core.vo;

/**
 * Token 刷新响应：新 Access Token + 原 Refresh Token。
 */
public record TokenVO(String accessToken, String refreshToken, String tokenType, long expiresIn) {
}
