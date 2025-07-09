package com.im.imcommunicationsystem.user.service.impl;

import com.im.imcommunicationsystem.auth.entity.LoginDevice;
import com.im.imcommunicationsystem.auth.repository.LoginDeviceRepository;
import com.im.imcommunicationsystem.user.dto.response.DeviceListResponse;
import com.im.imcommunicationsystem.user.service.UserDeviceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 用户设备服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDeviceServiceImpl implements UserDeviceService {

    private final LoginDeviceRepository loginDeviceRepository;

    @Override
    @Transactional(readOnly = true)
    public DeviceListResponse getUserDevices(Long userId) {
        log.info("获取用户设备列表: userId={}", userId);
        
        try {
            List<LoginDevice> devices = loginDeviceRepository.findByUserId(userId);
            List<LoginDevice> activeDevices = loginDeviceRepository.findByUserIdAndIsActiveTrue(userId);
            
            // 统计设备类型
            Map<String, Integer> deviceTypeStats = devices.stream()
                    .collect(Collectors.groupingBy(
                            LoginDevice::getDeviceType,
                            Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
                    ));
            
            return new DeviceListResponse(
                    userId,
                    devices,
                    devices.size(),
                    activeDevices.size(),
                    deviceTypeStats
            );
        } catch (Exception e) {
            log.error("获取用户设备列表失败: userId={}, error={}", userId, e.getMessage(), e);
            return new DeviceListResponse(userId, List.of(), 0, 0, new HashMap<>());
        }
    }

    @Override
    @Transactional
    public void removeUserDevice(Long userId, Long deviceId) {
        log.info("移除用户设备: userId={}, deviceId={}", userId, deviceId);
        
        try {
            Optional<LoginDevice> deviceOptional = loginDeviceRepository.findById(deviceId);
            if (deviceOptional.isPresent()) {
                LoginDevice device = deviceOptional.get();
                if (device.getUserId().equals(userId)) {
                    loginDeviceRepository.delete(device);
                    log.info("成功移除用户设备: userId={}, deviceId={}", userId, deviceId);
                } else {
                    log.warn("设备不属于该用户: userId={}, deviceId={}, deviceUserId={}", 
                            userId, deviceId, device.getUserId());
                }
            } else {
                log.warn("设备不存在: deviceId={}", deviceId);
            }
        } catch (Exception e) {
            log.error("移除用户设备失败: userId={}, deviceId={}, error={}", userId, deviceId, e.getMessage(), e);
            throw new RuntimeException("移除设备失败", e);
        }
    }

    @Override
    @Transactional
    public void updateDeviceInfo(Long userId, Long deviceId, String deviceInfo) {
        log.info("更新设备信息: userId={}, deviceId={}, deviceInfo={}", userId, deviceId, deviceInfo);
        
        try {
            Optional<LoginDevice> deviceOptional = loginDeviceRepository.findById(deviceId);
            if (deviceOptional.isPresent()) {
                LoginDevice device = deviceOptional.get();
                if (device.getUserId().equals(userId)) {
                    device.setDeviceInfo(deviceInfo);
                    loginDeviceRepository.save(device);
                    log.info("成功更新设备信息: userId={}, deviceId={}", userId, deviceId);
                } else {
                    log.warn("设备不属于该用户: userId={}, deviceId={}, deviceUserId={}", 
                            userId, deviceId, device.getUserId());
                }
            } else {
                log.warn("设备不存在: deviceId={}", deviceId);
            }
        } catch (Exception e) {
            log.error("更新设备信息失败: userId={}, deviceId={}, error={}", userId, deviceId, e.getMessage(), e);
            throw new RuntimeException("更新设备信息失败", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getDeviceStatistics(Long userId) {
        log.info("获取设备统计信息: userId={}", userId);
        
        try {
            List<LoginDevice> devices = loginDeviceRepository.findByUserId(userId);
            List<LoginDevice> activeDevices = loginDeviceRepository.findByUserIdAndIsActiveTrue(userId);
            
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("totalDevices", devices.size());
            statistics.put("activeDevices", activeDevices.size());
            statistics.put("inactiveDevices", devices.size() - activeDevices.size());
            
            // 设备类型统计
            Map<String, Integer> deviceTypeStats = devices.stream()
                    .collect(Collectors.groupingBy(
                            LoginDevice::getDeviceType,
                            Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
                    ));
            statistics.put("deviceTypeStats", deviceTypeStats);
            
            return statistics;
        } catch (Exception e) {
            log.error("获取设备统计信息失败: userId={}, error={}", userId, e.getMessage(), e);
            return new HashMap<>();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoginDevice> getActiveDevices(Long userId) {
        log.info("获取用户活跃设备列表: userId={}", userId);
        
        try {
            return loginDeviceRepository.findByUserIdAndIsActiveTrue(userId);
        } catch (Exception e) {
            log.error("获取用户活跃设备列表失败: userId={}, error={}", userId, e.getMessage(), e);
            return List.of();
        }
    }
}