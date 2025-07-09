package com.im.imcommunicationsystem.auth.controller;

import com.im.imcommunicationsystem.auth.dto.request.*;
import com.im.imcommunicationsystem.auth.service.PasswordService;
import com.im.imcommunicationsystem.common.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * 密码控制器
 * 处理密码相关的HTTP请求
 */
@RestController
@RequestMapping("/api/auth/password")
public class PasswordController {

    @Autowired
    private PasswordService passwordService;

    /**
     * 修改密码
     * @param request 修改密码请求
     * @return API响应
     */
    @PostMapping("/change")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        return passwordService.changePassword(request);
    }

    /**
     * 重置密码
     * @param request 重置密码请求
     * @return API响应
     */
    @PostMapping("/reset")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return passwordService.resetPassword(request);
    }
}