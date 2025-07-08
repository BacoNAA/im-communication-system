package com.im.imcommunicationsystem.auth.service;

import com.im.imcommunicationsystem.auth.entity.User;
import com.im.imcommunicationsystem.auth.dto.response.UserInfoResponse;
import com.im.imcommunicationsystem.common.dto.ApiResponse;

import java.util.Optional;

/**
 * 用户服务接口
 * 处理用户相关业务逻辑
 */
public interface UserService {

    /**
     * 根据邮箱查找用户
     * @param email 邮箱地址
     * @return 用户信息
     */
    Optional<User> findByEmail(String email);

    /**
     * 根据用户ID查找用户
     * @param userId 用户ID
     * @return 用户信息
     */
    Optional<User> findById(Long userId);

    /**
     * 根据用户ID字符串查找用户
     * @param userIdStr 用户ID字符串
     * @return 用户信息
     */
    Optional<User> findByUserIdStr(String userIdStr);

    /**
     * 创建新用户
     * @param user 用户信息
     * @return 创建的用户
     */
    User createUser(User user);

    /**
     * 更新用户信息
     * @param user 用户信息
     * @return 更新的用户
     */
    User updateUser(User user);

    /**
     * 检查邮箱是否存在
     * @param email 邮箱地址
     * @return 是否存在
     */
    boolean existsByEmail(String email);

    /**
     * 检查用户ID是否存在
     * @param userIdStr 用户ID字符串
     * @return 是否存在
     */
    boolean existsByUserIdStr(String userIdStr);

    /**
     * 生成唯一用户ID字符串
     * @return 用户ID字符串
     */
    String generateUniqueUserIdStr();

    /**
     * 转换为用户信息响应
     * @param user 用户实体
     * @return 用户信息响应
     */
    UserInfoResponse toUserInfoResponse(User user);
}