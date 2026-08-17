package com.zfc.eldercare.core.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 工具类（详细设计文档 5.1 / 8.1）。
 * 生成/解析双 Token，并封装 Redis 黑名单操作。
 */
@Component
public class JwtUtil {

    /** 单 Token 黑名单前缀：jwt:blacklist:{jti} */
    public static final String BLACKLIST_TOKEN_PREFIX = "jwt:blacklist:";
    /** 用户级黑名单前缀：jwt:blacklist:user:{userId} */
    public static final String BLACKLIST_USER_PREFIX = "jwt:blacklist:user:";

    private final SecretKey key;
    private final long accessExpireSeconds;
    private final long refreshExpireSeconds;
    private final RedisTemplate<String, Object> redisTemplate;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.access-token-expire}") long accessExpireSeconds,
                   @Value("${jwt.refresh-token-expire}") long refreshExpireSeconds,
                   RedisTemplate<String, Object> redisTemplate) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpireSeconds = accessExpireSeconds;
        this.refreshExpireSeconds = refreshExpireSeconds;
        this.redisTemplate = redisTemplate;
    }

    // ===== 生成 =====

    /** 生成 Access Token（2 小时，含 role 与 jti） */
    public String generateAccessToken(Long userId, String role) {
        return buildToken(userId, role, accessExpireSeconds);
    }

    /** 生成 Refresh Token（7 天，含 jti） */
    public String generateRefreshToken(Long userId) {
        return buildToken(userId, null, refreshExpireSeconds);
    }

    private String buildToken(Long userId, String role, long expireSeconds) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expireSeconds * 1000);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role)
                .id(UUID.randomUUID().toString().replace("-", ""))
                .issuedAt(now)
                .expiration(exp)
                .signWith(key)
                .compact();
    }

    // ===== 解析 =====

    /** 解析 Token 返回 Claims；非法或过期返回 null */
    public Claims parseClaims(String token) {
        try {
            return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    // ===== 黑名单（详细设计文档 8.1）=====

    /** 将单个 Token 加入黑名单，TTL 与 Token 剩余有效期一致 */
    public void addTokenToBlacklist(String jti, long remainingMillis) {
        redisTemplate.opsForValue().set(BLACKLIST_TOKEN_PREFIX + jti, "1", Duration.ofMillis(remainingMillis));
    }

    /** 强制下线所有设备：将用户加入黑名单，TTL 同 Refresh Token */
    public void addUserToBlacklist(Long userId) {
        redisTemplate.opsForValue().set(BLACKLIST_USER_PREFIX + userId, "1", Duration.ofSeconds(refreshExpireSeconds));
    }

    /** 用户重新登录成功后解除用户级黑名单（已重新认证，旧屏蔽不再生效） */
    public void removeUserBlacklist(Long userId) {
        redisTemplate.delete(BLACKLIST_USER_PREFIX + userId);
    }

    public boolean isTokenBlacklisted(String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_TOKEN_PREFIX + jti));
    }

    public boolean isUserBlacklisted(Long userId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_USER_PREFIX + userId));
    }

    // ===== Claims 取值 =====

    public Long getUserId(Claims claims) {
        return Long.valueOf(claims.getSubject());
    }

    public String getJti(Claims claims) {
        return claims.getId();
    }

    public String getRole(Claims claims) {
        return claims.get("role", String.class);
    }
}
