package com.im.imcommunicationsystem.user.controller;

import com.im.imcommunicationsystem.user.dto.request.UpdateProfileRequest;
import com.im.imcommunicationsystem.user.dto.request.SetUserIdRequest;
import com.im.imcommunicationsystem.user.dto.request.SetStatusRequest;
import com.im.imcommunicationsystem.user.dto.response.UserProfileResponse;
import com.im.imcommunicationsystem.user.service.UserProfileService;
import com.im.imcommunicationsystem.common.utils.SecurityUtils;
import com.im.imcommunicationsystem.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户资料控制器
 * 处理用户个人资料相关的HTTP请求
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final SecurityUtils securityUtils;

    /**
     * 获取用户个人资料
     * 
     * @return 用户资料信息
     */
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserProfile(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        UserProfileResponse profile = userProfileService.getUserProfile(userId);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    /**
     * 更新用户个人资料
     * 
     * @param request 更新资料请求
     * @return 操作结果
     */
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<Void>> updateUserProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            HttpServletRequest httpRequest) {
        Long userId = getCurrentUserId(httpRequest);
        userProfileService.updateUserProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 上传用户头像
     * 
     * @param file 头像文件
     * @return 头像URL
     */
    @PostMapping("/profile/avatar")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        String avatarUrl = userProfileService.updateAvatar(userId, file);
        
        Map<String, String> response = new HashMap<>();
        response.put("avatarUrl", avatarUrl);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 设置个人ID
     * 
     * @param request 设置用户ID请求
     * @return 操作结果
     */
    @PostMapping("/personal-id")
    public ResponseEntity<ApiResponse<Void>> setPersonalId(
            @Valid @RequestBody SetUserIdRequest request,
            HttpServletRequest httpRequest) {
        Long userId = getCurrentUserId(httpRequest);
        userProfileService.setPersonalId(userId, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 设置个性化状态
     * 
     * @param request 设置状态请求
     * @return 操作结果
     */
    @PostMapping("/status")
    public ResponseEntity<ApiResponse<Void>> setUserStatus(
            @Valid @RequestBody SetStatusRequest request,
            HttpServletRequest httpRequest) {
        Long userId = getCurrentUserId(httpRequest);
        userProfileService.setUserStatus(userId, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 清除个性化状态
     * 
     * @return 操作结果
     */
    @DeleteMapping("/status")
    public ResponseEntity<ApiResponse<Void>> clearUserStatus(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        userProfileService.clearUserStatus(userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
      * 从请求中获取当前用户ID
      * 
      * @param request HTTP请求
      * @return 用户ID
      */
     private Long getCurrentUserId(HttpServletRequest request) {
         return securityUtils.getCurrentUserId();
     }
}