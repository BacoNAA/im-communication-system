package com.im.imcommunicationsystem.common.filter;

import com.im.imcommunicationsystem.common.utils.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT认证过滤器
 * 用于验证JWT令牌并设置安全上下文
 * 
 * @author IM Team
 * @version 1.0
 * @since 2024-01-01
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        try {
            // 获取Authorization头
            String authHeader = request.getHeader("Authorization");
            
            // 检查是否包含Bearer令牌
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = jwtUtils.extractTokenFromHeader(authHeader);
                
                if (token != null && jwtUtils.validateAccessToken(token)) {
                    // 从令牌中提取用户信息
                    String username = jwtUtils.getUsernameFromToken(token);
                    Long userId = jwtUtils.getUserIdFromToken(token);
                    String roles = jwtUtils.getRolesFromToken(token);
                    
                    if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                        // 解析角色
                        List<SimpleGrantedAuthority> authorities = parseRoles(roles);
                        
                        // 创建认证对象
                        UsernamePasswordAuthenticationToken authToken = 
                            new UsernamePasswordAuthenticationToken(username, null, authorities);
                        
                        // 设置详细信息
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        
                        // 设置到安全上下文
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                        
                        log.debug("JWT认证成功，用户: {}, 用户ID: {}, 角色: {}", username, userId, roles);
                    }
                } else {
                    log.debug("JWT令牌无效或已过期");
                }
            }
        } catch (Exception e) {
            log.error("JWT认证过程中发生错误: {}", e.getMessage());
            // 清除安全上下文
            SecurityContextHolder.clearContext();
        }
        
        // 继续过滤器链
        filterChain.doFilter(request, response);
    }
    
    /**
     * 解析角色字符串为权限列表
     * 
     * @param roles 角色字符串，多个角色用逗号分隔
     * @return 权限列表
     */
    private List<SimpleGrantedAuthority> parseRoles(String roles) {
        if (roles == null || roles.trim().isEmpty()) {
            return List.of(new SimpleGrantedAuthority("ROLE_USER"));
        }
        
        return Arrays.stream(roles.split(","))
                .map(String::trim)
                .filter(role -> !role.isEmpty())
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }
    
    /**
     * 判断是否跳过JWT认证
     * 对于公开端点，跳过JWT认证
     * 
     * @param request HTTP请求
     * @return 是否跳过认证
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        
        // 公开端点，跳过JWT认证
        return path.startsWith("/api/auth/register/") ||
               path.startsWith("/api/auth/login/") ||
               path.startsWith("/api/auth/verification/") ||
               path.equals("/api/auth/password/reset") ||
               path.startsWith("/api/public/") ||
               path.startsWith("/api/test/") ||
               path.startsWith("/ws/") ||
               path.startsWith("/ws-native/") ||
               path.startsWith("/actuator/") ||
               path.startsWith("/swagger-ui/") ||
               path.startsWith("/v3/api-docs/") ||
               path.equals("/favicon.ico") ||
               path.equals("/error") ||
               path.equals("/") ||
               path.equals("/index.html") ||
               path.equals("/dashboard.html") ||
               path.startsWith("/static/") ||
               path.startsWith("/css/") ||
               path.startsWith("/js/") ||
               path.startsWith("/images/");
    }
}