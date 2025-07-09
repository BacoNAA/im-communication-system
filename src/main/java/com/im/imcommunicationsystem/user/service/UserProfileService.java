package com.im.imcommunicationsystem.user.service;

import com.im.imcommunicationsystem.user.dto.request.UpdateProfileRequest;
import com.im.imcommunicationsystem.user.dto.request.SetUserIdRequest;
import com.im.imcommunicationsystem.user.dto.request.SetStatusRequest;
import com.im.imcommunicationsystem.user.dto.response.UserProfileResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户资料服务接口
 * 实现用户个人资料管理的核心业务逻辑
 */
public interface UserProfileService {

    /**
     * 获取用户完整资料
     * 
     * @param userId 用户ID
     * @return 用户资料响应
     */
    UserProfileResponse getUserProfile(Long userId);

    /**
     * 更新用户资料
     * 
     * @param userId 用户ID
     * @param request 更新资料请求
     */
    void updateUserProfile(Long userId, UpdateProfileRequest request);

    /**
     * 更新用户头像
     * 
     * @param userId 用户ID
     * @param file 头像文件
     * @return 头像URL
     */
    String updateAvatar(Long userId, MultipartFile file);

    /**
     * 设置个人ID（检查唯一性）
     * 
     * @param userId 用户ID
     * @param request 设置用户ID请求
     */
    void setPersonalId(Long userId, SetUserIdRequest request);

    /**
     * 验证个人ID格式和可用性
     * 
     * @param userIdStr 个人ID字符串
     * @return 是否可用
     */
    boolean validateUserIdStr(String userIdStr);

    /**
     * 设置个性化状态
     * 
     * @param userId 用户ID
     * @param request 设置状态请求
     */
    void setUserStatus(Long userId, SetStatusRequest request);

    /**
     * 清除个性化状态
     * 
     * @param userId 用户ID
     */
    void clearUserStatus(Long userId);
}