package com.im.imcommunicationsystem.user.service;

import com.im.imcommunicationsystem.user.dto.request.SetStatusRequest;

import java.util.Map;

/**
 * 用户状态服务接口
 * 实现用户个性化状态管理的业务逻辑
 */
public interface UserStatusService {

    /**
     * 设置用户状态
     * 
     * @param userId 用户ID
     * @param request 设置状态请求
     */
    void setUserStatus(Long userId, SetStatusRequest request);

    /**
     * 获取用户状态
     * 
     * @param userId 用户ID
     * @return 用户状态信息
     */
    Map<String, Object> getUserStatus(Long userId);

    /**
     * 清除用户状态
     * 
     * @param userId 用户ID
     */
    void clearUserStatus(Long userId);

    /**
     * 验证状态内容
     * 
     * @param request 状态请求
     * @return 是否有效
     */
    boolean validateStatusContent(SetStatusRequest request);

    /**
     * 检查状态是否过期
     * 
     * @param userId 用户ID
     * @return 是否过期
     */
    boolean isStatusExpired(Long userId);
}