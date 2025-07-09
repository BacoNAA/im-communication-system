package com.im.imcommunicationsystem.common.utils;

import com.im.imcommunicationsystem.auth.entity.User;
import com.im.imcommunicationsystem.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;

/**
 * 安全工具类
 * 用于获取当前登录用户信息
 * 
 * @author IM Team
 * @version 1.0
 * @since 2024-01-01
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;

    /**
     * 获取当前登录用户的用户名
     * 
     * @return 用户名，如果未登录则返回null
     */
    public String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && 
            !"anonymousUser".equals(authentication.getPrincipal())) {
            return authentication.getName();
        }
        return null;
    }

    /**
     * 获取当前登录用户的用户ID
     * 
     * @return 用户ID，如果未登录则返回null
     */
    public Long getCurrentUserId() {
        try {
            // 首先尝试从JWT token中获取用户ID
            String token = getCurrentToken();
            if (token != null) {
                Long userId = jwtUtils.getUserIdFromToken(token);
                if (userId != null) {
                    return userId;
                }
            }
            
            // 如果从token中获取不到，则通过用户名查询数据库
            String username = getCurrentUsername();
            if (username != null) {
                Optional<User> userOptional = userRepository.findByEmail(username);
                if (userOptional.isPresent()) {
                    return userOptional.get().getId();
                }
            }
            
            return null;
        } catch (Exception e) {
            log.error("获取当前用户ID失败", e);
            return null;
        }
    }

    /**
     * 获取当前登录用户信息
     * 
     * @return 用户信息，如果未登录则返回null
     */
    public User getCurrentUser() {
        try {
            Long userId = getCurrentUserId();
            if (userId != null) {
                Optional<User> userOptional = userRepository.findById(userId);
                return userOptional.orElse(null);
            }
            return null;
        } catch (Exception e) {
            log.error("获取当前用户信息失败", e);
            return null;
        }
    }

    /**
     * 检查用户是否已登录
     * 
     * @return 是否已登录
     */
    public boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated() && 
               !"anonymousUser".equals(authentication.getPrincipal());
    }

    /**
     * 从请求头中获取当前的JWT token
     * 
     * @return JWT token，如果没有则返回null
     */
    private String getCurrentToken() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    return jwtUtils.extractTokenFromHeader(authHeader);
                }
            }
            return null;
        } catch (Exception e) {
            log.debug("获取当前token失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 检查当前用户是否有指定角色
     * 
     * @param role 角色名称
     * @return 是否有该角色
     */
    public boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getAuthorities() != null) {
            String roleWithPrefix = role.startsWith("ROLE_") ? role : "ROLE_" + role;
            return authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals(roleWithPrefix));
        }
        return false;
    }

    /**
     * 检查当前用户是否有任意一个指定角色
     * 
     * @param roles 角色名称数组
     * @return 是否有任意一个角色
     */
    public boolean hasAnyRole(String... roles) {
        for (String role : roles) {
            if (hasRole(role)) {
                return true;
            }
        }
        return false;
    }
}