package com.im.imcommunicationsystem.user.controller;

import com.im.imcommunicationsystem.user.dto.request.UpdateSettingsRequest;
import com.im.imcommunicationsystem.user.dto.response.UserSettingsResponse;
import com.im.imcommunicationsystem.user.service.UserSettingsService;
import com.im.imcommunicationsystem.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * 用户设置控制器
 * 处理用户个性化设置相关的HTTP请求
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserSettingsController {

    private final UserSettingsService userSettingsService;

    /**
     * 获取用户设置
     * 
     * @return 用户设置信息
     */
    @GetMapping("/settings")
    public ResponseEntity<ApiResponse<UserSettingsResponse>> getUserSettings() {
        // 从JWT token中获取用户ID
        Long userId = 1L; // 暂时使用固定用户ID
        
        UserSettingsResponse response = userSettingsService.getUserSettings(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 更新用户设置
     * 
     * @param request 更新设置请求
     * @return 操作结果
     */
    @PutMapping("/settings")
    public ResponseEntity<ApiResponse<Void>> updateUserSettings(@Valid @RequestBody UpdateSettingsRequest request) {
        // 从JWT token中获取用户ID
        Long userId = 1L; // 暂时使用固定用户ID
        
        userSettingsService.updateUserSettings(userId, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 重置为默认设置
     * 
     * @return 操作结果
     */
    @PostMapping("/settings/reset")
    public ResponseEntity<ApiResponse<Void>> resetToDefault() {
        // 从JWT token中获取用户ID
        Long userId = 1L; // 暂时使用固定用户ID
        
        userSettingsService.resetToDefaultSettings(userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}