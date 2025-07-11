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
            // 标准化设备类型大小写
            String normalizedDeviceType = normalizeDeviceType(deviceType);
            log.debug("设备类型标准化: {} -> {}", deviceType, normalizedDeviceType);
            
            // 查找是否已存在该设备（根据设备信息）
            Optional<LoginDevice> existingDevice = loginDeviceRepository
                    .findByUserIdAndDeviceInfo(userId, deviceInfo);
            
            LoginDevice device;
            if (existingDevice.isPresent()) {
                // 更新现有设备信息
                device = existingDevice.get();
                device.setDeviceType(normalizedDeviceType); // 确保设备类型也被更新
                device.setDeviceInfo(deviceInfo);
                device.setIpAddress(ipAddress);
                device.setLastLoginAt(LocalDateTime.now());
                device.setIsActive(true);
                log.info("更新现有设备记录: deviceId={}", device.getId());
            } else {
                // 创建新设备记录
                device = LoginDevice.builder()
                        .userId(userId)
                        .deviceType(normalizedDeviceType)
                        .deviceInfo(deviceInfo)
                        .ipAddress(ipAddress)
                        .lastLoginAt(LocalDateTime.now())
                        .isActive(true)
                        .build();
                log.info("创建新设备记录");
            }
            
            try {
                return loginDeviceRepository.save(device);
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                // 处理唯一约束冲突，可能是并发插入导致的
                log.warn("检测到数据库约束冲突，尝试重新查询设备: userId={}, deviceInfo={}", userId, deviceInfo);
                
                // 重新查询，可能是并发操作导致的
                Optional<LoginDevice> retryDevice = loginDeviceRepository
                        .findByUserIdAndDeviceInfo(userId, deviceInfo);
                
                if (retryDevice.isPresent()) {
                    // 更新找到的设备
                    LoginDevice foundDevice = retryDevice.get();
                    foundDevice.setDeviceType(normalizedDeviceType);
                    foundDevice.setIpAddress(ipAddress);
                    foundDevice.setLastLoginAt(LocalDateTime.now());
                    foundDevice.setIsActive(true);
                    log.info("并发冲突解决，更新设备记录: deviceId={}", foundDevice.getId());
                    return loginDeviceRepository.save(foundDevice);
                } else {
                    log.error("数据库约束冲突但无法找到冲突的设备记录: userId={}, deviceInfo={}", userId, deviceInfo);
                    throw e;
                }
            }
            
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            log.error("数据库约束冲突: userId={}, deviceType={}, deviceInfo={}, error={}", 
                    userId, deviceType, deviceInfo, e.getMessage());
            throw new RuntimeException("设备记录保存失败，可能存在重复数据", e);
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
    @Transactional
    public void logoutDevice(Long userId, String deviceType) {
        log.info("设备登出: userId={}, deviceType={}", userId, deviceType);
        try {
            String normalizedDeviceType = normalizeDeviceType(deviceType);
            // 登出该用户该设备类型的所有活跃设备
            List<LoginDevice> activeDevices = loginDeviceRepository.findByUserIdAndDeviceTypeAndIsActiveTrue(userId, normalizedDeviceType);
            if (!activeDevices.isEmpty()) {
                for (LoginDevice device : activeDevices) {
                    device.setIsActive(false);
                }
                loginDeviceRepository.saveAll(activeDevices);
                log.info("设备已成功登出: userId={}, deviceType={}, loggedOutCount={}", userId, deviceType, activeDevices.size());
            } else {
                log.warn("未找到要登出的活跃设备: userId={}, deviceType={}", userId, deviceType);
            }
        } catch (Exception e) {
            log.error("设备登出失败: userId={}, deviceType={}, error={}", userId, deviceType, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public void logoutAllDevices(Long userId) {
        log.info("所有设备登出: userId={}", userId);
        try {
            List<LoginDevice> activeDevices = loginDeviceRepository.findByUserIdAndIsActiveTrue(userId);
            for (LoginDevice device : activeDevices) {
                device.setIsActive(false);
            }
            loginDeviceRepository.saveAll(activeDevices);
            log.info("已成功登出用户所有设备: userId={}, deviceCount={}", userId, activeDevices.size());
        } catch (Exception e) {
            log.error("登出所有设备失败: userId={}, error={}", userId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public void updateDeviceStatus(Long userId, String deviceType, boolean isActive) {
        log.info("更新设备状态: userId={}, deviceType={}, isActive={}", userId, deviceType, isActive);
        try {
            String normalizedDeviceType = normalizeDeviceType(deviceType);
            // 如果要设置为活跃状态，只更新最新的设备记录
            if (isActive) {
                Optional<LoginDevice> device = loginDeviceRepository.findByUserIdAndDeviceType(userId, normalizedDeviceType);
                if (device.isPresent()) {
                    LoginDevice loginDevice = device.get();
                    loginDevice.setIsActive(true);
                    loginDeviceRepository.save(loginDevice);
                    log.info("设备状态已更新为活跃: deviceId={}", loginDevice.getId());
                } else {
                    log.warn("未找到要更新状态的设备: userId={}, deviceType={}", userId, deviceType);
                }
            } else {
                // 如果要设置为非活跃状态，更新所有该类型的设备
                List<LoginDevice> devices = loginDeviceRepository.findByUserIdAndDeviceTypeAndIsActiveTrue(userId, normalizedDeviceType);
                if (!devices.isEmpty()) {
                    for (LoginDevice device : devices) {
                        device.setIsActive(false);
                    }
                    loginDeviceRepository.saveAll(devices);
                    log.info("设备状态已更新为非活跃: userId={}, deviceType={}, updatedCount={}", userId, deviceType, devices.size());
                } else {
                    log.warn("未找到要更新状态的活跃设备: userId={}, deviceType={}", userId, deviceType);
                }
            }
        } catch (Exception e) {
            log.error("更新设备状态失败: userId={}, deviceType={}, isActive={}, error={}", 
                    userId, deviceType, isActive, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public boolean isDeviceActive(Long userId, String deviceType) {
        log.info("检查设备是否活跃: userId={}, deviceType={}", userId, deviceType);
        try {
            String normalizedDeviceType = normalizeDeviceType(deviceType);
            List<LoginDevice> activeDevices = loginDeviceRepository.findByUserIdAndDeviceTypeAndIsActiveTrue(userId, normalizedDeviceType);
            boolean isActive = !activeDevices.isEmpty();
            log.info("设备活跃状态检查结果: userId={}, deviceType={}, isActive={}, activeDeviceCount={}", 
                    userId, deviceType, isActive, activeDevices.size());
            return isActive;
        } catch (Exception e) {
            log.error("检查设备活跃状态失败: userId={}, deviceType={}, error={}", userId, deviceType, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean isDeviceSessionValid(Long userId, String deviceType) {
        log.debug("检查设备会话有效性: userId={}, deviceType={}", userId, deviceType);
        
        if (userId == null || deviceType == null || deviceType.trim().isEmpty()) {
            log.debug("设备会话检查参数无效: userId={}, deviceType={}", userId, deviceType);
            return false;
        }
        
        try {
            String normalizedDeviceType = normalizeDeviceType(deviceType);
            // 查找该用户该设备类型的所有活跃设备
            List<LoginDevice> activeDevices = loginDeviceRepository.findByUserIdAndDeviceTypeAndIsActiveTrue(userId, normalizedDeviceType);
            
            if (activeDevices.isEmpty()) {
                log.debug("未找到活跃的设备记录: userId={}, deviceType={}", userId, deviceType);
                return false;
            }
            
            // 如果有活跃设备，则会话有效
            boolean isValid = !activeDevices.isEmpty();
            log.debug("设备会话有效性检查结果: userId={}, deviceType={}, isValid={}, activeDeviceCount={}", 
                    userId, deviceType, isValid, activeDevices.size());
            return isValid;
        } catch (Exception e) {
            log.error("检查设备会话有效性时发生错误: userId={}, deviceType={}, error={}", userId, deviceType, e.getMessage());
            return false;
        }
     }

    @Override
    @Transactional
    public void logoutDeviceByInfo(Long userId, String deviceInfo) {
        log.info("设备登出（根据设备信息）: userId={}, deviceInfo={}", userId, deviceInfo);
        try {
            Optional<LoginDevice> device = loginDeviceRepository.findByUserIdAndDeviceInfo(userId, deviceInfo);
            if (device.isPresent()) {
                LoginDevice loginDevice = device.get();
                loginDevice.setIsActive(false);
                loginDeviceRepository.save(loginDevice);
                log.info("设备已成功登出: deviceId={}", loginDevice.getId());
            } else {
                log.warn("未找到要登出的设备: userId={}, deviceInfo={}", userId, deviceInfo);
            }
        } catch (Exception e) {
            log.error("设备登出失败: userId={}, deviceInfo={}, error={}", userId, deviceInfo, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public void updateDeviceStatusByInfo(Long userId, String deviceInfo, boolean isActive) {
        log.info("更新设备状态（根据设备信息）: userId={}, deviceInfo={}, isActive={}", userId, deviceInfo, isActive);
        try {
            Optional<LoginDevice> device = loginDeviceRepository.findByUserIdAndDeviceInfo(userId, deviceInfo);
            if (device.isPresent()) {
                LoginDevice loginDevice = device.get();
                loginDevice.setIsActive(isActive);
                loginDeviceRepository.save(loginDevice);
                log.info("设备状态已更新: deviceId={}, isActive={}", loginDevice.getId(), isActive);
            } else {
                log.warn("未找到要更新状态的设备: userId={}, deviceInfo={}", userId, deviceInfo);
            }
        } catch (Exception e) {
            log.error("更新设备状态失败: userId={}, deviceInfo={}, isActive={}, error={}", 
                    userId, deviceInfo, isActive, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public boolean isDeviceActiveByInfo(Long userId, String deviceInfo) {
        log.info("检查设备是否活跃（根据设备信息）: userId={}, deviceInfo={}", userId, deviceInfo);
        try {
            Optional<LoginDevice> device = loginDeviceRepository.findByUserIdAndDeviceInfo(userId, deviceInfo);
            boolean isActive = device.map(LoginDevice::getIsActive).orElse(false);
            log.info("设备活跃状态检查结果: userId={}, deviceInfo={}, isActive={}", userId, deviceInfo, isActive);
            return isActive;
        } catch (Exception e) {
            log.error("检查设备活跃状态失败: userId={}, deviceInfo={}, error={}", userId, deviceInfo, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean isDeviceSessionValidByInfo(Long userId, String deviceInfo) {
        log.debug("检查设备会话有效性（根据设备信息）: userId={}, deviceInfo={}", userId, deviceInfo);
        
        if (userId == null || deviceInfo == null || deviceInfo.trim().isEmpty()) {
            log.debug("设备会话检查参数无效: userId={}, deviceInfo={}", userId, deviceInfo);
            return false;
        }
        
        try {
            Optional<LoginDevice> device = loginDeviceRepository.findByUserIdAndDeviceInfo(userId, deviceInfo);
            if (device.isEmpty()) {
                log.debug("设备记录不存在: userId={}, deviceInfo={}", userId, deviceInfo);
                return false;
            }
            
            LoginDevice loginDevice = device.get();
            boolean isValid = Boolean.TRUE.equals(loginDevice.getIsActive());
            log.debug("设备会话有效性检查结果: userId={}, deviceInfo={}, isValid={}", userId, deviceInfo, isValid);
            return isValid;
        } catch (Exception e) {
            log.error("检查设备会话有效性时发生错误: userId={}, deviceInfo={}, error={}", userId, deviceInfo, e.getMessage());
            return false;
        }
    }
     
     /**
      * 标准化设备类型大小写
     * 确保设备类型使用统一的首字母大写格式
     * 
     * @param deviceType 原始设备类型
     * @return 标准化后的设备类型
     */
    private String normalizeDeviceType(String deviceType) {
        if (deviceType == null || deviceType.trim().isEmpty()) {
            return "Web"; // 默认设备类型
        }
        
        String normalized = deviceType.trim().toLowerCase();
        switch (normalized) {
            case "web":
                return "Web";
            case "mobile":
                return "Mobile";
            case "tablet":
                return "Tablet";
            case "desktop":
                return "Desktop";
            case "android":
                return "Android";
            case "ios":
                return "iOS";
            case "windows":
                return "Windows";
            case "mac":
                return "Mac";
            case "linux":
                return "Linux";
            default:
                // 对于未知类型，使用首字母大写的格式
                return deviceType.substring(0, 1).toUpperCase() + deviceType.substring(1).toLowerCase();
        }
    }
}