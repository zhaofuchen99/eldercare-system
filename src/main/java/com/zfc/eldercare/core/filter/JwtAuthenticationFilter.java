package com.zfc.eldercare.core.filter;

import com.zfc.eldercare.core.service.RbacService;
import com.zfc.eldercare.core.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * JWT 认证过滤器（详细设计文档 8.2）。
 * 从 Authorization: Bearer {token} 解析 Access Token，校验黑名单后填充 SecurityContext。
 * 认证后加载 RBAC 角色集与权限集（Redis 缓存），角色级 hasRole 与权限级 hasAuthority 均可判定。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final RbacService rbacService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);
        if (token != null) {
            Claims claims = jwtUtil.parseClaims(token);
            if (claims != null
                    && !jwtUtil.isTokenBlacklisted(jwtUtil.getJti(claims))
                    && !jwtUtil.isUserBlacklisted(jwtUtil.getUserId(claims))) {
                Long userId = jwtUtil.getUserId(claims);
                String jwtRole = jwtUtil.getRole(claims);
                List<SimpleGrantedAuthority> authorities = buildAuthorities(userId, jwtRole);
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userId, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        filterChain.doFilter(request, response);
    }

    /**
     * 组装 authority：RBAC 解析的角色码（ROLE_x）+ 权限码；user_role 为空时回退 JWT role claim。
     * 解析异常 fail-open（仅用 JWT role），保持路径级 hasRole 拦截行为不因缓存/DB 故障而全部中断。
     */
    private List<SimpleGrantedAuthority> buildAuthorities(Long userId, String jwtRole) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        try {
            Set<String> roles = rbacService.rolesOf(userId);
            if (roles.isEmpty() && jwtRole != null) {
                roles = Set.of(jwtRole);
            }
            roles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
            rbacService.permissionsOf(userId)
                    .forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission)));
        } catch (Exception e) {
            log.warn("RBAC 权限解析失败，回退到 JWT role，userId={}", userId, e);
            if (jwtRole != null) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + jwtRole));
            }
        }
        return authorities;
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(AUTH_HEADER);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
