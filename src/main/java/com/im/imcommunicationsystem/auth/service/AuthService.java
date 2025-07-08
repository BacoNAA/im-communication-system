package com.im.imcommunicationsystem.auth.service;

import com.im.imcommunicationsystem.auth.dto.request.*;
import com.im.imcommunicationsystem.auth.dto.response.*;

/**
 * 认证服务接口
 * 处理用户认证相关业务逻辑
 */
public interface AuthService {

    /**
     * 邮箱注册
     * @param request 邮箱注册请求
     * @return 认证响应
     */
    AuthResponse registerByEmail(EmailRegistrationRequest request);

    /**
     * 密码登录
     * @param request 密码登录请求
     * @return 认证响应
     */
    AuthResponse loginByPassword(PasswordLoginRequest request);

    /**
     * 验证码登录
     * @param request 验证码登录请求
     * @return 认证响应
     */
    AuthResponse loginByVerificationCode(VerificationCodeLoginRequest request);

    /**
     * 验证器登录（二次验证）
     * @param request 验证器登录请求
     * @return 认证响应
     */
    AuthResponse loginByAuthenticator(AuthenticatorLoginRequest request);

    /**
     * 用户登出
     * @param request 登出请求
     */
    void logout(LogoutRequest request);

    /**
     * 刷新访问令牌
     * @param refreshToken 刷新令牌
     * @return 认证响应
     */
    AuthResponse refreshToken(String refreshToken);

    /**
     * 获取用户信息
     * @param userId 用户ID
     * @return 用户信息响应
     */
    UserInfoResponse getUserInfo(Long userId);

    /**
     * 检查邮箱是否已存在
     * @param email 邮箱地址
     * @return 是否存在
     */
    boolean checkEmailExists(String email);
}