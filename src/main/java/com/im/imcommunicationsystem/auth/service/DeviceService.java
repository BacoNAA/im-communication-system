package com.im.imcommunicationsystem.auth.service;

import com.im.imcommunicationsystem.auth.entity.LoginDevice;

import java.util.List;

/**
 * 设备服务接口
 * 处理登录设备相关业务逻辑
 */
public interface DeviceService {

    /**
     * 记录登录设备
     * @param userId 用户ID
     * @param deviceType 设备类型
     * @param deviceInfo 设备信息
     * @param ipAddress IP地址
     * @return 登录设备信息
     */
    LoginDevice recordLoginDevice(Long userId, String deviceType, String deviceInfo, String ipAddress);

    /**
     * 获取用户活跃设备列表
     * @param userId 用户ID
     * @return 活跃设备列表
     */
    List<LoginDevice> getActiveDevices(Long userId);

    /**
     * 注销指定设备
     * @param userId 用户ID
     * @param deviceType 设备类型
     */
    void logoutDevice(Long userId, String deviceType);

    /**
     * 注销所有设备
     * @param userId 用户ID
     */
    void logoutAllDevices(Long userId);

    /**
     * 更新设备活跃状态
     * @param userId 用户ID
     * @param deviceType 设备类型
     * @param isActive 是否活跃
     */
    void updateDeviceStatus(Long userId, String deviceType, boolean isActive);

    /**
     * 检查设备是否处于活跃状态
     * @param userId 用户ID
     * @param deviceType 设备类型
     * @return 是否活跃
     */
    boolean isDeviceActive(Long userId, String deviceType);
    
    /**
     * 检查用户设备会话是否有效
     * @param userId 用户ID
     * @param deviceType 设备类型
     * @return 会话是否有效
     */
    boolean isDeviceSessionValid(Long userId, String deviceType);

    /**
     * 注销指定设备（根据设备信息）
     * @param userId 用户ID
     * @param deviceInfo 设备信息
     */
    void logoutDeviceByInfo(Long userId, String deviceInfo);

    /**
     * 更新设备活跃状态（根据设备信息）
     * @param userId 用户ID
     * @param deviceInfo 设备信息
     * @param isActive 是否活跃
     */
    void updateDeviceStatusByInfo(Long userId, String deviceInfo, boolean isActive);

    /**
     * 检查设备是否处于活跃状态（根据设备信息）
     * @param userId 用户ID
     * @param deviceInfo 设备信息
     * @return 是否活跃
     */
    boolean isDeviceActiveByInfo(Long userId, String deviceInfo);

    /**
     * 检查用户设备会话是否有效（根据设备信息）
     * @param userId 用户ID
     * @param deviceInfo 设备信息
     * @return 会话是否有效
     */
    boolean isDeviceSessionValidByInfo(Long userId, String deviceInfo);

    /**
     * 更新设备的最后活跃时间
     * 用于保持设备在线状态
     * @param userId 用户ID
     * @param deviceInfo 设备信息
     */
    void updateDeviceLastActiveTime(Long userId, String deviceInfo);

    /**
     * 根据设备类型更新最后活跃时间
     * @param userId 用户ID
     * @param deviceType 设备类型
     */
    void updateDeviceLastActiveTimeByType(Long userId, String deviceType);
}