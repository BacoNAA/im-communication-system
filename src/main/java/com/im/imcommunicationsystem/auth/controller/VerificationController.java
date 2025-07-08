package com.im.imcommunicationsystem.auth.controller;

import com.im.imcommunicationsystem.auth.service.VerificationService;
import com.im.imcommunicationsystem.common.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 验证控制器
 * 处理验证码相关的HTTP请求
 */
@RestController
@RequestMapping("/api/auth/verification")
public class VerificationController {

    @Autowired
    private VerificationService verificationService;

    /**
     * 发送注册验证码
     * @param email 邮箱地址
     * @return API响应
     */
    @PostMapping("/send/register")
    public ApiResponse<Void> sendRegistrationCode(@RequestParam String email) {
        try {
            return verificationService.sendRegistrationCode(email);
        } catch (Exception e) {
            return ApiResponse.serverError("发送验证码失败: " + e.getMessage());
        }
    }

    /**
     * 发送登录验证码
     * @param email 邮箱地址
     * @return API响应
     */
    @PostMapping("/send/login")
    public ApiResponse<Void> sendLoginCode(@RequestParam String email) {
        try {
            return verificationService.sendLoginCode(email);
        } catch (Exception e) {
            return ApiResponse.serverError("发送验证码失败: " + e.getMessage());
        }
    }

    /**
     * 发送密码重置验证码
     * @param email 邮箱地址
     * @return API响应
     */
    @PostMapping("/send/reset-password")
    public ApiResponse<Void> sendPasswordResetCode(@RequestParam String email) {
        try {
            return verificationService.sendPasswordResetCode(email);
        } catch (Exception e) {
            return ApiResponse.serverError("发送验证码失败: " + e.getMessage());
        }
    }

    /**
     * 验证验证码
     * @param email 邮箱地址
     * @param code 验证码
     * @param codeType 验证码类型
     * @return API响应
     */
    @PostMapping("/verify")
    public ApiResponse<Boolean> verifyCode(@RequestParam String email, 
                                          @RequestParam String code, 
                                          @RequestParam String codeType) {
        try {
            boolean isValid = verificationService.verifyCode(email, code, codeType);
            return ApiResponse.success(isValid);
        } catch (Exception e) {
            return ApiResponse.serverError("验证失败: " + e.getMessage());
        }
    }
}