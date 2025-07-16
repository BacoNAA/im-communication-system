package com.im.imcommunicationsystem.auth.service;

import com.im.imcommunicationsystem.auth.repository.LoginDeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 设备状态管理器
 * 负责在应用启动时重置设备状态，以及定期清理过期设备
 * 
 * @author IM Team
 * @version 1.0
 * @since 2024-01-01
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceStatusManager {

    private final LoginDeviceRepository loginDeviceRepository;

    /**
     * 应用启动完成后重置所有设备状态
     * 服务器重启时，所有设备都应该被标记为离线状态
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void resetAllDeviceStatusOnStartup() {
        log.info("应用启动完成，开始重置所有设备状态...");
        
        try {
            // 将所有活跃设备标记为非活跃状态
            int updatedCount = loginDeviceRepository.updateAllActiveDevicesToInactive();
            log.info("应用启动时设备状态重置完成，共重置 {} 台设备为离线状态", updatedCount);
        } catch (Exception e) {
            log.error("重置设备状态失败", e);
        }
    }

    /**
     * 定时清理长时间未活跃的设备记录
     * 每天凌晨2点执行，清理30天前的非活跃设备记录
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupInactiveDevices() {
        log.info("开始清理长时间未活跃的设备记录...");
        
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(30);
            int deletedCount = loginDeviceRepository.deleteInactiveDevicesOlderThan(cutoffTime);
            log.info("设备记录清理完成，共删除 {} 条30天前的非活跃设备记录", deletedCount);
        } catch (Exception e) {
            log.error("清理设备记录失败", e);
        }
    }

    /**
     * 定时检查并更新设备活跃状态
     * 每小时执行一次，将超过24小时未更新的设备标记为非活跃
     */
    @Scheduled(fixedRate = 3600000) // 每小时执行一次
    @Transactional
    public void updateStaleDeviceStatus() {
        log.debug("开始检查并更新过期设备状态...");
        
        try {
            LocalDateTime staleTime = LocalDateTime.now().minusHours(24);
            int updatedCount = loginDeviceRepository.updateStaleDevicesToInactive(staleTime);
            
            if (updatedCount > 0) {
                log.info("设备状态更新完成，共将 {} 台超过24小时未活跃的设备标记为离线", updatedCount);
            }
        } catch (Exception e) {
            log.error("更新设备状态失败", e);
        }
    }

    /**
     * 手动重置所有设备状态（管理员功能）
     */
    @Transactional
    public int resetAllDeviceStatus() {
        log.info("手动重置所有设备状态...");
        
        try {
            int updatedCount = loginDeviceRepository.updateAllActiveDevicesToInactive();
            log.info("手动重置设备状态完成，共重置 {} 台设备为离线状态", updatedCount);
            return updatedCount;
        } catch (Exception e) {
            log.error("手动重置设备状态失败", e);
            throw e;
        }
    }
}