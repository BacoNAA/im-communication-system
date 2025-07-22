package com.im.imcommunicationsystem.common.filter;

import com.im.imcommunicationsystem.auth.service.DeviceService;
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
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
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
    private final DeviceService deviceService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        try {
            // 获取Authorization头
            String authHeader = request.getHeader("Authorization");
            Long userId = null;
            boolean isAuthenticated = false;
            
            // 首先尝试通过Authorization头认证
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = jwtUtils.extractTokenFromHeader(authHeader);
                
                if (token != null && jwtUtils.validateAccessToken(token)) {
                    // 从令牌中提取用户信息
                    String username = jwtUtils.getUsernameFromToken(token);
                    userId = jwtUtils.getUserIdFromToken(token);
                    String roles = jwtUtils.getRolesFromToken(token);
                    String deviceType = jwtUtils.getDeviceTypeFromToken(token);
                    
                    if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                        // 检查设备会话是否有效
                        if (deviceType != null && !deviceService.isDeviceSessionValid(userId, deviceType)) {
                            log.warn("设备会话已失效，拒绝访问: userId={}, deviceType={}", userId, deviceType);
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"error\":\"DEVICE_SESSION_INVALID\",\"message\":\"设备会话已失效，请重新登录\"}");
                            return;
                        }
                        
                        // 解析角色
                        List<SimpleGrantedAuthority> authorities = parseRoles(roles);
                        
                        // 详细打印角色信息，方便调试
                        log.info("用户角色信息 - 原始角色字符串: {}", roles);
                        log.info("解析后的权限列表: {}", authorities);
                        
                        // 创建认证对象
                        UsernamePasswordAuthenticationToken authToken = 
                            new UsernamePasswordAuthenticationToken(username, null, authorities);
                        
                        // 设置详细信息
                        authToken.setDetails(request.getRemoteAddr());
                        
                        // 设置到安全上下文
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                        
                        // 标记认证成功
                        isAuthenticated = true;
                        
                        log.debug("JWT认证成功，用户: {}, 用户ID: {}, 角色: {}, 设备: {}", username, userId, roles, deviceType);
                    }
                } else {
                    log.debug("JWT令牌无效或已过期");
                }
            }
            
            // 如果JWT认证失败，尝试其他认证方式
            if (!isAuthenticated) {
                // 尝试从X-User-Id头获取用户ID
                String userIdHeader = request.getHeader("X-User-Id");
                if (StringUtils.hasText(userIdHeader)) {
                    try {
                        userId = Long.parseLong(userIdHeader);
                        log.debug("从X-User-Id头获取用户ID: {}", userId);
                    } catch (NumberFormatException e) {
                        log.warn("X-User-Id头无效: {}", userIdHeader);
                    }
                }
                
                // 尝试从URL参数获取用户ID
                if (userId == null) {
                    String userIdParam = request.getParameter("userId");
                    if (StringUtils.hasText(userIdParam)) {
                        try {
                            userId = Long.parseLong(userIdParam);
                            log.debug("从URL参数获取用户ID: {}", userId);
                        } catch (NumberFormatException e) {
                            log.warn("userId参数无效: {}", userIdParam);
                        }
                    }
                }
            }
            
            // 设置用户ID属性
            if (userId != null) {
                request.setAttribute("userId", userId);
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
                .map(role -> {
                    // 保留原始角色名的大小写，只添加前缀
                    if (role.startsWith("ROLE_")) {
                        return role;
                    } else {
                        return "ROLE_" + role;
                    }
                })
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
               path.startsWith("/api/media/public/") || // 允许公开访问媒体文件
               path.startsWith("/api/test/") ||
               path.equals("/api/admin/auth/login") || // 允许管理员登录接口的公开访问
               path.equals("/api/admin/auth/reset-password") || // 允许管理员重置密码接口的公开访问（测试用）
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
               
        // 注意: /api/moments/upload/images 需要认证，不在跳过列表中
    }
}