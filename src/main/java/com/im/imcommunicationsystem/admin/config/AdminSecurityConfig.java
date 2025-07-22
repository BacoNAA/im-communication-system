package com.im.imcommunicationsystem.admin.config;

import com.im.imcommunicationsystem.common.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 管理模块安全配置
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class AdminSecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * 配置管理API端点的安全
     *
     * @param http HttpSecurity对象
     * @return 配置好的SecurityFilterChain
     * @throws Exception 配置过程中出现错误时抛出
     */
    @Bean
    @Order(1)
    public SecurityFilterChain adminFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/admin/**")
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/api/admin/auth/login").permitAll() // 允许管理员登录接口的公开访问
                .requestMatchers("/api/admin/auth/reset-password").permitAll() // 允许重置密码接口的公开访问（仅用于测试）
                .anyRequest().hasAnyRole("ADMIN", "admin", "SUPER_ADMIN", "super_admin")
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .csrf(csrf -> csrf.disable())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class); // 添加JWT过滤器

        return http.build();
    }
} 