package com.im.imcommunicationsystem.auth.controller;

import com.im.imcommunicationsystem.auth.dto.request.*;
import com.im.imcommunicationsystem.auth.dto.response.*;
import com.im.imcommunicationsystem.auth.service.AuthService;
import com.im.imcommunicationsystem.common.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * 认证控制器
 * 处理用户认证相关的HTTP请求
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * 邮箱注册
     * @param request 邮箱注册请求
     * @return 认证响应
     */
    @PostMapping("/register/email")
    public ApiResponse<AuthResponse> registerByEmail(@Valid @RequestBody EmailRegistrationRequest request) {
        try {
            AuthResponse authResponse = authService.registerByEmail(request);
            return ApiResponse.success("注册成功", authResponse);
        } catch (Exception e) {
            return ApiResponse.serverError("注册失败: " + e.getMessage());
        }
    }

    /**
     * 密码登录
     * @param request 密码登录请求
     * @return 认证响应
     */
    @PostMapping("/login/password")
    public ApiResponse<AuthResponse> loginByPassword(@Valid @RequestBody PasswordLoginRequest request) {
        AuthResponse authResponse = authService.loginByPassword(request);
        return ApiResponse.success("登录成功", authResponse);
    }

    /**
     * 验证码登录
     * @param request 验证码登录请求
     * @return 认证响应
     */
    @PostMapping("/login/verification-code")
    public ApiResponse<AuthResponse> loginByVerificationCode(@Valid @RequestBody VerificationCodeLoginRequest request) {
        try {
            AuthResponse authResponse = authService.loginByVerificationCode(request);
            return ApiResponse.success("登录成功", authResponse);
        } catch (Exception e) {
            return ApiResponse.serverError("登录失败: " + e.getMessage());
        }
    }

    /**
     * 验证器登录（二次验证）
     * @param request 验证器登录请求
     * @return 认证响应
     */
    @PostMapping("/login/authenticator")
    public ApiResponse<AuthResponse> loginByAuthenticator(@Valid @RequestBody AuthenticatorLoginRequest request) {
        // TODO: 实现验证器登录逻辑
        return null;
    }

    /**
     * 用户登出
     * @param request 登出请求
     * @return 登出响应
     */
    @PostMapping("/logout")
    public ApiResponse<String> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request);
        return ApiResponse.success("登出成功");
    }

    /**
     * 刷新访问令牌
     * @param refreshToken 刷新令牌
     * @return 认证响应
     */
    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refreshToken(@RequestParam String refreshToken) {
        // TODO: 实现刷新令牌逻辑
        return null;
    }

    /**
     * 检查邮箱是否已存在
     * @param email 邮箱地址
     * @return 检查结果
     */
    @GetMapping("/check-email")
    public ApiResponse<EmailCheckResponse> checkEmailExists(@RequestParam String email) {
        try {
            boolean exists = authService.checkEmailExists(email);
            EmailCheckResponse response = EmailCheckResponse.builder()
                    .exists(exists)
                    .build();
            return ApiResponse.success("检查完成", response);
        } catch (Exception e) {
            return ApiResponse.serverError("检查邮箱失败: " + e.getMessage());
        }
    }

    /**
     * 获取当前用户信息
     * @return 用户信息响应
     */
    @GetMapping("/me")
    public ApiResponse<UserInfoResponse> getCurrentUser() {
        try {
            UserInfoResponse userInfo = authService.getCurrentUserInfo();
            return ApiResponse.success("获取用户信息成功", userInfo);
        } catch (Exception e) {
            return ApiResponse.unauthorized("获取用户信息失败: " + e.getMessage());
        }
    }
}