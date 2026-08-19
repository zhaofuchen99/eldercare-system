package com.zfc.eldercare.core.config;

import com.zfc.eldercare.core.filter.JwtAuthenticationFilter;
import com.zfc.eldercare.core.security.RestAccessDeniedHandler;
import com.zfc.eldercare.core.security.RestAuthenticationEntryPoint;
import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.DispatcherTypeRequestMatcher;

/**
 * Spring Security 配置（详细设计文档 8.2）。
 * 路径级权限：/api/member/** 需 MEMBER（ADMIN 也可访问）；/api/admin/** 需 ADMIN；认证接口放行。
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    @Bean
    @Order(1)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**", "/api/sms/**","/test").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/member/**").hasAnyRole("MEMBER", "ADMIN")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .httpBasic(h -> h.disable())
                .formLogin(f -> f.disable());
        return http.build();
    }

    /**
     * 异步分派（ASYNC dispatch）专用安全链。
     * SSE（SseEmitter）流结束/超时/客户端断开时，Tomcat 会发起异步分派重进过滤器链；
     * 此时 JwtAuthenticationFilter 因 once-per-request 标记被跳过，STATELESS 会话下 SecurityContext
     * 也不会跨线程传递，AuthorizationFilter 会将匿名请求误判为拒绝并抛出 Access Denied。
     * 原始 REQUEST 已完成认证授权，异步收尾分派统一放行即可。
     */
    @Bean
    @Order(0)
    public SecurityFilterChain asyncSecurityFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher(new DispatcherTypeRequestMatcher(DispatcherType.ASYNC))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    /**
     * 禁用 JwtAuthenticationFilter 的全局 servlet 自动注册（文档 8.2）。
     * 过滤器是 @Component 会被 Spring Boot 注册到全局过滤器链，运行在 Security 链之前，
     * 其写入的 SecurityContext 会被链内 SecurityContextHolderFilter 的 deferred context 覆盖导致 401。
     * 这里只让它通过上方 addFilterBefore 在 Security 链内部运行（位于 SecurityContextHolderFilter 之后）。
     */
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtAuthenticationFilterRegistration(JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    /** 密码编码器：BCrypt（文档 5.1 密码安全） */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
