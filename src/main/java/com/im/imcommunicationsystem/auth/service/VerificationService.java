package com.im.imcommunicationsystem.auth.service;

import com.im.imcommunicationsystem.common.dto.ApiResponse;

/**
 * 验证服务接口
 * 处理验证码相关业务逻辑
 */
public interface VerificationService {

    /**
     * 发送注册验证码
     * @param email 邮箱地址
     * @return API响应
     */
    ApiResponse<Void> sendRegistrationCode(String email);

    /**
     * 发送登录验证码
     * @param email 邮箱地址
     * @return API响应
     */
    ApiResponse<Void> sendLoginCode(String email);

    /**
     * 发送密码重置验证码
     * @param email 邮箱地址
     * @return API响应
     */
    ApiResponse<Void> sendPasswordResetCode(String email);

    /**
     * 验证验证码
     * @param email 邮箱地址
     * @param code 验证码
     * @param codeType 验证码类型
     * @return 是否验证成功
     */
    boolean verifyCode(String email, String code, String codeType);

    /**
     * 生成验证码
     * @return 验证码
     */
    String generateCode();

    /**
     * 清理过期验证码
     */
    void cleanExpiredCodes();

    /**
     * 删除验证码
     * @param email 邮箱地址
     * @param codeType 验证码类型
     */
    void deleteVerificationCode(String email, String codeType);
}