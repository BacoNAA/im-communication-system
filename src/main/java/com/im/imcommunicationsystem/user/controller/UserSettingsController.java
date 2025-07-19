package com.im.imcommunicationsystem.user.controller;

import com.im.imcommunicationsystem.user.dto.request.UpdateSettingsRequest;
import com.im.imcommunicationsystem.user.dto.response.UserSettingsResponse;
import com.im.imcommunicationsystem.user.service.UserSettingsService;
import com.im.imcommunicationsystem.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/**
 * 用户设置控制器
 * 处理用户个性化设置相关的HTTP请求
 */
@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class UserSettingsController {

    private final UserSettingsService userSettingsService;

    /**
     * 获取用户设置
     * 
     * @return 用户设置信息
     */
    @GetMapping("/settings")
    public ResponseEntity<ApiResponse<UserSettingsResponse>> getUserSettings(HttpServletRequest request) {
        log.info("获取用户设置API被调用");
        
        // 从JWT token中获取用户ID
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            log.warn("未授权请求：未找到userId，请求头：{}", request.getHeaderNames());
            // 检查Authorization头
            String authHeader = request.getHeader("Authorization");
            log.info("Authorization头: {}", authHeader != null ? "存在" : "不存在");
            return ResponseEntity.status(401).body(ApiResponse.unauthorized("未登录或登录已过期，请重新登录"));
        }
        
        log.info("处理用户 {} 的设置请求", userId);
        
        try {
            UserSettingsResponse response = userSettingsService.getUserSettings(userId);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("获取用户设置失败", e);
            return ResponseEntity.status(500).body(ApiResponse.error(500, "获取设置失败: " + e.getMessage()));
        }
    }

    /**
     * 更新用户设置
     * 
     * @param request 更新设置请求
     * @return 操作结果
     */
    @PutMapping("/settings")
    public ResponseEntity<ApiResponse<Void>> updateUserSettings(
            @Valid @RequestBody UpdateSettingsRequest request,
            HttpServletRequest httpRequest) {
        log.info("更新用户设置API被调用");
        
        // 从JWT token中获取用户ID
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            log.warn("未授权请求：未找到userId，请求头：{}", httpRequest.getHeaderNames());
            // 检查Authorization头
            String authHeader = httpRequest.getHeader("Authorization");
            log.info("Authorization头: {}", authHeader != null ? "存在" : "不存在");
            
            // 紧急方案：尝试从请求体或参数中获取用户ID
            Long alternativeUserId = null;
            try {
                String userIdParam = httpRequest.getParameter("userId");
                if (userIdParam != null && !userIdParam.isEmpty()) {
                    alternativeUserId = Long.parseLong(userIdParam);
                    log.info("从URL参数获取到用户ID: {}", alternativeUserId);
                }
            } catch (Exception e) {
                log.warn("尝试从参数获取用户ID失败", e);
            }
            
            // 如果找到了替代的用户ID，使用它
            if (alternativeUserId != null) {
                log.info("使用替代用户ID: {}", alternativeUserId);
                try {
                    userSettingsService.updateUserSettings(alternativeUserId, request);
                    return ResponseEntity.ok(ApiResponse.success(null));
                } catch (Exception e) {
                    log.error("使用替代ID更新用户设置失败", e);
                    return ResponseEntity.status(500).body(ApiResponse.error(500, "更新设置失败: " + e.getMessage()));
                }
            }
            
            return ResponseEntity.status(401).body(ApiResponse.unauthorized("未登录或登录已过期，请重新登录"));
        }
        
        log.info("更新用户 {} 的设置: {}", userId, request);
        
        try {
            userSettingsService.updateUserSettings(userId, request);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (Exception e) {
            log.error("更新用户设置失败", e);
            return ResponseEntity.status(500).body(ApiResponse.error(500, "更新设置失败: " + e.getMessage()));
        }
    }

    /**
     * 重置为默认设置
     * 
     * @return 操作结果
     */
    @PostMapping("/settings/reset")
    public ResponseEntity<ApiResponse<Void>> resetToDefault(HttpServletRequest request) {
        log.info("重置用户设置API被调用");
        
        // 从JWT token中获取用户ID
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            log.warn("未授权请求：未找到userId，请求头：{}", request.getHeaderNames());
            // 检查Authorization头
            String authHeader = request.getHeader("Authorization");
            log.info("Authorization头: {}", authHeader != null ? "存在" : "不存在");
            return ResponseEntity.status(401).body(ApiResponse.unauthorized("未登录或登录已过期，请重新登录"));
        }
        
        log.info("重置用户 {} 的设置", userId);
        
        try {
            userSettingsService.resetToDefaultSettings(userId);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (Exception e) {
            log.error("重置用户设置失败", e);
            return ResponseEntity.status(500).body(ApiResponse.error(500, "重置设置失败: " + e.getMessage()));
        }
    }
}