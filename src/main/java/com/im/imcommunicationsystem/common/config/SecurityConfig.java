package com.im.imcommunicationsystem.common.config;

import com.im.imcommunicationsystem.common.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;

/**
 * Spring Security配置类
 * 配置JWT认证、权限控制和CORS
 * 
 * @author IM Team
 * @version 1.0
 * @since 2024-01-01
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * 密码编码器
     * 使用BCrypt加密算法
     * 
     * @return PasswordEncoder实例
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 配置安全过滤器链
     * 
     * @param http HttpSecurity配置对象
     * @return SecurityFilterChain实例
     * @throws Exception 配置异常
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 禁用CSRF保护（因为使用JWT）
                .csrf(AbstractHttpConfigurer::disable)
                
                // 配置CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                
                // 配置会话管理为无状态
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                
                // 配置请求授权
                .authorizeHttpRequests(authz -> authz
                        // 公开访问的端点
                        .requestMatchers(
                                "/api/auth/register/**",    // 注册相关接口
                                "/api/auth/login/**",       // 登录相关接口
                                "/api/auth/verification/**", // 验证码相关接口
                                "/api/auth/password/reset",  // 重置密码接口（公开）
                                "/api/verification/**",   // 验证码相关接口
                                "/api/account-lock/**",   // 账号锁定管理接口
                                "/api/public/**",        // 公开接口
                                "/api/placeholder/**",   // 占位符图片接口
                                "/api/test/**",          // 测试接口
                                "/ws/**",                // WebSocket端点
                                "/ws-native/**",         // 原生WebSocket端点
                                "/actuator/**",          // 监控端点
                                "/swagger-ui/**",        // Swagger UI
                                "/v3/api-docs/**",       // OpenAPI文档
                                "/favicon.ico",          // 网站图标
                                "/error",                // 错误页面
                                "/",                     // 根路径
                                "/index.html",           // 首页
                                "/register.html",        // 注册页面
                                "/login.html",           // 登录页面
                                "/dashboard.html",       // 仪表板页面
                                "/static/**",            // 静态资源
                                "/css/**",               // CSS文件
                                "/js/**",                // JavaScript文件
                                "/images/**",            // 图片文件
                                "/*.html"                // 所有HTML文件
                        ).permitAll()
                        
                        // 管理员接口需要ADMIN权限
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        
                        // 其他所有请求都需要认证
                        .anyRequest().authenticated()
                )
                
                // 配置异常处理
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(401);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write(
                                    "{\"code\":401,\"message\":\"未授权访问，请先登录\",\"data\":null}"
                            );
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(403);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write(
                                    "{\"code\":403,\"message\":\"权限不足，拒绝访问\",\"data\":null}"
                            );
                        })
                );

        // 添加JWT认证过滤器
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 配置CORS
     * 允许跨域请求
     * 
     * @return CorsConfigurationSource实例
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 允许的源
        configuration.setAllowedOriginPatterns(Collections.singletonList("*"));
        
        // 允许的HTTP方法
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        
        // 允许的请求头
        configuration.setAllowedHeaders(Collections.singletonList("*"));
        
        // 允许携带凭证
        configuration.setAllowCredentials(true);
        
        // 预检请求的缓存时间
        configuration.setMaxAge(3600L);
        
        // 暴露的响应头
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }

    /**
     * 安全配置说明：
     * 
     * 1. 认证方式：
     *    - JWT Token认证
     *    - 无状态会话管理
     *    - BCrypt密码加密
     * 
     * 2. 权限控制：
     *    - ROLE_USER: 普通用户权限
     *    - ROLE_ADMIN: 管理员权限
     *    - ROLE_MODERATOR: 版主权限
     * 
     * 3. 公开接口：
     *    - /api/auth/**: 用户注册、登录、验证码等
     *    - /api/public/**: 公开信息查询
     *    - /ws/**: WebSocket连接端点
     *    - /actuator/**: 应用监控端点
     * 
     * 4. 受保护接口：
     *    - /api/user/**: 用户相关操作（需要USER权限）
     *    - /api/message/**: 消息相关操作（需要USER权限）
     *    - /api/group/**: 群组相关操作（需要USER权限）
     *    - /api/admin/**: 管理员操作（需要ADMIN权限）
     * 
     * 5. JWT Token格式：
     *    - Header: Authorization: Bearer <token>
     *    - Payload包含: userId, username, roles, exp等
     *    - 过期时间: 24小时（可配置）
     * 
     * 6. 安全特性：
     *    - CSRF保护已禁用（适用于API）
     *    - CORS已配置支持跨域
     *    - 密码使用BCrypt加密
     *    - 支持方法级权限控制
     */

}