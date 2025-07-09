package com.im.imcommunicationsystem.user.service;

import com.im.imcommunicationsystem.user.dto.request.UpdateSettingsRequest;
import com.im.imcommunicationsystem.user.dto.response.UserSettingsResponse;

/**
 * 用户设置服务接口
 * 实现用户个性化设置的业务逻辑
 */
public interface UserSettingsService {

    /**
     * 获取用户设置
     * 
     * @param userId 用户ID
     * @return 用户设置响应
     */
    UserSettingsResponse getUserSettings(Long userId);

    /**
     * 更新用户设置
     * 
     * @param userId 用户ID
     * @param request 更新设置请求
     */
    void updateUserSettings(Long userId, UpdateSettingsRequest request);

    /**
     * 合并设置项
     * 
     * @param userId 用户ID
     * @param request 更新设置请求
     */
    void mergeSettings(Long userId, UpdateSettingsRequest request);

    /**
     * 重置为默认设置
     * 
     * @param userId 用户ID
     */
    void resetToDefaultSettings(Long userId);

    /**
     * 验证设置参数
     * 
     * @param request 设置请求
     * @return 是否有效
     */
    boolean validateSettings(UpdateSettingsRequest request);
}