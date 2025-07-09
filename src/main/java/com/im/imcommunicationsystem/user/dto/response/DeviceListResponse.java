package com.im.imcommunicationsystem.user.dto.response;

import com.im.imcommunicationsystem.auth.entity.LoginDevice;

import java.util.List;
import java.util.Map;

/**
 * 设备列表响应DTO
 */
public class DeviceListResponse {

    private Long userId;
    private List<LoginDevice> devices;
    private int totalCount;
    private int activeCount;
    private Map<String, Integer> deviceTypeStats;

    // 构造函数
    public DeviceListResponse() {}

    public DeviceListResponse(Long userId, List<LoginDevice> devices, int totalCount, 
                            int activeCount, Map<String, Integer> deviceTypeStats) {
        this.userId = userId;
        this.devices = devices;
        this.totalCount = totalCount;
        this.activeCount = activeCount;
        this.deviceTypeStats = deviceTypeStats;
    }

    // Getter和Setter方法
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public List<LoginDevice> getDevices() {
        return devices;
    }

    public void setDevices(List<LoginDevice> devices) {
        this.devices = devices;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public int getActiveCount() {
        return activeCount;
    }

    public void setActiveCount(int activeCount) {
        this.activeCount = activeCount;
    }

    public Map<String, Integer> getDeviceTypeStats() {
        return deviceTypeStats;
    }

    public void setDeviceTypeStats(Map<String, Integer> deviceTypeStats) {
        this.deviceTypeStats = deviceTypeStats;
    }

    @Override
    public String toString() {
        return "DeviceListResponse{" +
                "userId=" + userId +
                ", devices=" + devices +
                ", totalCount=" + totalCount +
                ", activeCount=" + activeCount +
                ", deviceTypeStats=" + deviceTypeStats +
                '}';
    }
}