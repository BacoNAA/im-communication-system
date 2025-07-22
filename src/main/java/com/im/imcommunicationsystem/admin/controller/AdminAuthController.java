package com.im.imcommunicationsystem.admin.controller;

import com.im.imcommunicationsystem.admin.dto.request.AdminLoginRequest;
import com.im.imcommunicationsystem.admin.dto.request.AdminResetPasswordRequest;
import com.im.imcommunicationsystem.admin.dto.response.AdminAuthResponse;
import com.im.imcommunicationsystem.admin.service.AdminAuthService;
import com.im.imcommunicationsystem.common.dto.ApiResponse;
import com.im.imcommunicationsystem.common.utils.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员认证控制器
 */
@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
@Slf4j
public class AdminAuthController {

    private final AdminAuthService adminAuthService;
    private final SecurityUtils securityUtils;

    /**
     * 管理员登录
     * @param request 登录请求
     * @return 认证响应
     */
    @PostMapping("/login")
    public ApiResponse<AdminAuthResponse> login(@Valid @RequestBody AdminLoginRequest request) {
        log.info("管理员登录请求: {}", request.getUsername());
        AdminAuthResponse response = adminAuthService.login(request);
        return ApiResponse.success("登录成功", response);
    }

    /**
     * 管理员登出
     * @return 登出响应
     */
    @PostMapping("/logout")
    public ApiResponse<String> logout() {
        Long adminId = securityUtils.getCurrentUserId();
        if (adminId != null) {
            log.info("管理员登出请求: adminId={}", adminId);
            adminAuthService.logout(adminId);
        }
        return ApiResponse.success("登出成功", "操作成功");
    }

    /**
     * 获取管理员信息
     * @return 管理员信息
     */
    @GetMapping("/info")
    public ApiResponse<AdminAuthResponse.AdminInfo> getAdminInfo() {
        Long adminId = securityUtils.getCurrentUserId();
        if (adminId == null) {
            return ApiResponse.error(401, "未登录");
        }
        
        log.info("获取管理员信息: adminId={}", adminId);
        AdminAuthResponse.AdminInfo adminInfo = adminAuthService.getAdminInfo(adminId);
        return ApiResponse.success("获取成功", adminInfo);
    }
    
    /**
     * 重置管理员密码
     * 注意：仅用于测试，生产环境中应该移除或增加更严格的认证
     * 
     * @param request 重置密码请求
     * @return 响应结果
     */
    @PostMapping("/reset-password")
    public ApiResponse<String> resetPassword(@Valid @RequestBody AdminResetPasswordRequest request) {
        log.info("接收到重置管理员密码请求: {}", request.getUsername());
        try {
            // 检查参数
            if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
                log.warn("用户名为空");
                return ApiResponse.error(400, "用户名不能为空");
            }
            
            // 检查密码强度
            if (request.getNewPassword() == null || request.getNewPassword().length() < 6) {
                log.warn("密码强度不足: {}", request.getUsername());
                return ApiResponse.error(400, "密码长度不能少于6位");
            }
            
            log.info("开始处理重置密码请求: {}", request.getUsername());
            boolean result = adminAuthService.resetPassword(request.getUsername(), request.getNewPassword());
            
            if (result) {
                log.info("重置密码成功: {}", request.getUsername());
                return ApiResponse.success("密码重置成功", "密码已更新");
            } else {
                log.warn("重置密码失败，管理员不存在: {}", request.getUsername());
                return ApiResponse.error(404, "管理员不存在");
            }
        } catch (RuntimeException e) {
            log.error("重置管理员密码失败(运行时异常): {}", e.getMessage(), e);
            return ApiResponse.error(500, "重置密码过程中发生错误: " + e.getMessage());
        } catch (Exception e) {
            log.error("重置管理员密码失败(其他异常): {}", e.getMessage(), e);
            return ApiResponse.error(500, "系统错误，请稍后重试");
        }
    }
} 