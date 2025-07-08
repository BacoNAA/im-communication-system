package com.im.imcommunicationsystem.auth.service;

import com.im.imcommunicationsystem.auth.dto.request.*;
import com.im.imcommunicationsystem.common.dto.ApiResponse;

/**
 * 密码服务接口
 * 处理密码相关业务逻辑
 */
public interface PasswordService {

    /**
     * 修改密码
     * @param request 修改密码请求
     * @return API响应
     */
    ApiResponse<Void> changePassword(ChangePasswordRequest request);

    /**
     * 重置密码
     * @param request 重置密码请求
     * @return API响应
     */
    ApiResponse<Void> resetPassword(ResetPasswordRequest request);

    /**
     * 验证当前密码
     * @param userId 用户ID
     * @param currentPassword 当前密码
     * @return 是否验证成功
     */
    boolean validateCurrentPassword(Long userId, String currentPassword);

    /**
     * 加密密码
     * @param rawPassword 原始密码
     * @return 加密后的密码
     */
    String encodePassword(String rawPassword);
}