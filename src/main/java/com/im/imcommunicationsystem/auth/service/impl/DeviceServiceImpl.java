package com.im.imcommunicationsystem.auth.service.impl;

import com.im.imcommunicationsystem.auth.entity.LoginDevice;
import com.im.imcommunicationsystem.auth.repository.LoginDeviceRepository;
import com.im.imcommunicationsystem.auth.service.DeviceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 设备服务实现类
 * 处理登录设备相关业务逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {

    private final LoginDeviceRepository loginDeviceRepository;

    @Override
    @Transactional
    public LoginDevice recordLoginDevice(Long userId, String deviceType, String deviceInfo, String ipAddress) {
        log.info("记录登录设备: userId={}, deviceType={}, ipAddress={}", userId, deviceType, ipAddress);
        
        try {
            // 查找是否已存在该设备
            Optional<LoginDevice> existingDevice = loginDeviceRepository
                    .findByUserIdAndDeviceType(userId, deviceType);
            
            LoginDevice device;
            if (existingDevice.isPresent()) {
                // 更新现有设备信息
                device = existingDevice.get();
                device.setDeviceInfo(deviceInfo);
                device.setIpAddress(ipAddress);
                device.setLastLoginAt(LocalDateTime.now());
                device.setIsActive(true);
                log.info("更新现有设备记录: deviceId={}", device.getId());
            } else {
                // 创建新设备记录
                device = LoginDevice.builder()
                        .userId(userId)
                        .deviceType(deviceType)
                        .deviceInfo(deviceInfo)
                        .ipAddress(ipAddress)
                        .lastLoginAt(LocalDateTime.now())
                        .isActive(true)
                        .build();
                log.info("创建新设备记录");
            }
            
            return loginDeviceRepository.save(device);
            
        } catch (Exception e) {
            log.error("记录登录设备失败: userId={}, deviceType={}, error={}", 
                    userId, deviceType, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public List<LoginDevice> getActiveDevices(Long userId) {
        log.info("获取用户活跃设备列表: userId={}", userId);
        try {
            return loginDeviceRepository.findByUserIdAndIsActiveTrue(userId);
        } catch (Exception e) {
            log.error("获取用户活跃设备列表失败: userId={}, error={}", userId, e.getMessage(), e);
            return List.of();
        }
    }

    @Override
    public void logoutDevice(Long userId, String deviceType) {
        // 注意：设备登出功能暂不实现，仅保留接口
        log.info("设备登出: userId={}, deviceType={}", userId, deviceType);
    }

    @Override
    public void logoutAllDevices(Long userId) {
        // 注意：所有设备登出功能暂不实现，仅保留接口
        log.info("所有设备登出: userId={}", userId);
    }

    @Override
    public void updateDeviceStatus(Long userId, String deviceType, boolean isActive) {
        // 注意：设备状态更新功能暂不实现，仅保留接口
        log.info("更新设备状态: userId={}, deviceType={}, isActive={}", userId, deviceType, isActive);
    }

    @Override
    public boolean isDeviceActive(Long userId, String deviceType) {
        // 注意：设备活跃状态检查功能暂不实现，仅保留接口
        log.info("检查设备是否活跃: userId={}, deviceType={}", userId, deviceType);
        return true;
    }
}