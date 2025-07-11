package com.im.imcommunicationsystem.user.service;

import com.im.imcommunicationsystem.user.dto.response.DeviceListResponse;
import com.im.imcommunicationsystem.auth.entity.LoginDevice;

import java.util.List;
import java.util.Map;

/**
 * 用户设备服务接口
 * 实现用户设备管理的业务逻辑
 */
public interface UserDeviceService {

    /**
     * 获取用户所有设备
     * 
     * @param userId 用户ID
     * @return 设备列表响应
     */
    DeviceListResponse getUserDevices(Long userId);

    /**
     * 强制下线用户设备
     * 
     * @param userId 用户ID
     * @param deviceId 设备ID
     */
    void removeUserDevice(Long userId, Long deviceId);

    /**
     * 更新设备信息
     * 
     * @param userId 用户ID
     * @param deviceId 设备ID
     * @param deviceInfo 设备信息
     */
    void updateDeviceInfo(Long userId, Long deviceId, String deviceInfo);

    /**
     * 获取设备统计信息
     * 
     * @param userId 用户ID
     * @return 设备统计信息
     */
    Map<String, Object> getDeviceStatistics(Long userId);

    /**
     * 获取用户活跃设备列表
     * 
     * @param userId 用户ID
     * @return 活跃设备列表
     */
    List<LoginDevice> getActiveDevices(Long userId);

    /**
     * 强制下线所有其他设备
     * 
     * @param userId 用户ID
     * @param currentDeviceInfo 当前设备信息
     * @return 下线设备数量
     */
    int logoutAllOtherDevices(Long userId, String currentDeviceInfo);
}