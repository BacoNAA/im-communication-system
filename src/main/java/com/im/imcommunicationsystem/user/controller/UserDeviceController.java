package com.im.imcommunicationsystem.user.controller;

import com.im.imcommunicationsystem.user.dto.response.DeviceListResponse;
import com.im.imcommunicationsystem.user.service.UserDeviceService;
import com.im.imcommunicationsystem.common.dto.ApiResponse;
import com.im.imcommunicationsystem.common.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 设备管理控制器
 * 处理用户设备管理相关的HTTP请求
 */
@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserDeviceController {

    private final UserDeviceService userDeviceService;
    private final SecurityUtils securityUtils;

    /**
     * 获取用户设备列表
     * 
     * @return 设备列表信息
     */
    @GetMapping("/devices")
    public ResponseEntity<ApiResponse<DeviceListResponse>> getUserDevices() {
        try {
            Long userId = securityUtils.getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.ok(ApiResponse.error(401, "用户未登录"));
            }
            
            DeviceListResponse deviceList = userDeviceService.getUserDevices(userId);
            return ResponseEntity.ok(ApiResponse.success(deviceList));
        } catch (Exception e) {
            log.error("获取用户设备列表失败", e);
            return ResponseEntity.ok(ApiResponse.error(500, "获取设备列表失败: " + e.getMessage()));
        }
    }

    /**
     * 强制下线指定设备
     * 
     * @param deviceId 设备ID
     * @return 操作结果
     */
    @DeleteMapping("/devices/{deviceId}")
    public ResponseEntity<ApiResponse<Void>> removeDevice(@PathVariable Long deviceId) {
        try {
            Long userId = securityUtils.getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.ok(ApiResponse.error(401, "用户未登录"));
            }
            
            userDeviceService.removeUserDevice(userId, deviceId);
            return ResponseEntity.ok(ApiResponse.success("设备已成功下线", null));
        } catch (Exception e) {
            log.error("强制下线设备失败: deviceId={}", deviceId, e);
            return ResponseEntity.ok(ApiResponse.error(500, "强制下线失败: " + e.getMessage()));
        }
    }

    /**
     * 更新设备信息
     * 
     * @param deviceId 设备ID
     * @param deviceInfo 设备信息
     * @return 操作结果
     */
    @PutMapping("/devices/{deviceId}")
    public ResponseEntity<ApiResponse<Void>> updateDeviceInfo(
            @PathVariable Long deviceId, 
            @RequestParam String deviceInfo) {
        try {
            Long userId = securityUtils.getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.ok(ApiResponse.error(401, "用户未登录"));
            }
            
            userDeviceService.updateDeviceInfo(userId, deviceId, deviceInfo);
            return ResponseEntity.ok(ApiResponse.success("设备信息更新成功", null));
        } catch (Exception e) {
            log.error("更新设备信息失败: deviceId={}", deviceId, e);
            return ResponseEntity.ok(ApiResponse.error(500, "更新设备信息失败: " + e.getMessage()));
        }
    }

    /**
     * 强制下线所有其他设备
     * 
     * @param currentDeviceInfo 当前设备信息
     * @return 操作结果
     */
    @PostMapping("/devices/logout-others")
    public ResponseEntity<ApiResponse<Integer>> logoutAllOtherDevices(
            @RequestParam String currentDeviceInfo) {
        try {
            Long userId = securityUtils.getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.ok(ApiResponse.error(401, "用户未登录"));
            }
            
            int logoutCount = userDeviceService.logoutAllOtherDevices(userId, currentDeviceInfo);
            String message = logoutCount > 0 ? 
                String.format("已成功下线 %d 台其他设备", logoutCount) : 
                "当前只有本设备在线，无其他设备需要下线";
            return ResponseEntity.ok(ApiResponse.success(message, logoutCount));
        } catch (Exception e) {
            log.error("强制下线所有其他设备失败: currentDeviceInfo={}", currentDeviceInfo, e);
            return ResponseEntity.ok(ApiResponse.error(500, "强制下线所有其他设备失败: " + e.getMessage()));
        }
    }
}