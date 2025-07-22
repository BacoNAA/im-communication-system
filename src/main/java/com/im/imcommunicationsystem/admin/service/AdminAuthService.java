package com.im.imcommunicationsystem.admin.service;

import com.im.imcommunicationsystem.admin.dto.request.AdminLoginRequest;
import com.im.imcommunicationsystem.admin.dto.response.AdminAuthResponse;

/**
 * 管理员认证服务接口
 * 处理管理员认证相关业务逻辑
 */
public interface AdminAuthService {

    /**
     * 管理员登录
     * @param request 登录请求
     * @return 认证响应
     */
    AdminAuthResponse login(AdminLoginRequest request);

    /**
     * 管理员登出
     * @param adminId 管理员ID
     */
    void logout(Long adminId);

    /**
     * 获取管理员信息
     * @param adminId 管理员ID
     * @return 管理员信息
     */
    AdminAuthResponse.AdminInfo getAdminInfo(Long adminId);

    /**
     * 验证管理员密码
     * @param adminId 管理员ID
     * @param password 密码
     * @return 是否验证成功
     */
    boolean validatePassword(Long adminId, String password);

    /**
     * 更新管理员最后登录时间
     * @param adminId 管理员ID
     */
    void updateLastLoginTime(Long adminId);
    
    /**
     * 重置管理员密码
     * @param username 管理员用户名
     * @param newPassword 新密码
     * @return 是否重置成功
     */
    boolean resetPassword(String username, String newPassword);
} 