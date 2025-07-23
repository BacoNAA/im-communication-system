package com.im.imcommunicationsystem.admin.service;

import com.im.imcommunicationsystem.admin.dto.response.StatisticsResponse;
import com.im.imcommunicationsystem.admin.dto.response.SystemOverviewResponse;

import java.util.Map;

/**
 * 管理员仪表盘服务接口
 */
public interface AdminDashboardService {
    
    /**
     * 获取系统概览数据
     * @param period 时间周期 (today, week, month, year)
     * @return 系统概览数据
     */
    SystemOverviewResponse getSystemOverview(String period);
    
    /**
     * 获取统计数据
     * @param type 统计类型 (users, messages, groups, moments)
     * @param period 时间周期 (today, week, month, year)
     * @return 统计数据
     */
    StatisticsResponse getStatistics(String type, String period);
    
    /**
     * 获取用户活跃度分布
     * @param period 时间周期 (today, week, month, year)
     * @return 用户活跃度分布数据
     */
    Map<String, Object> getUserActivityDistribution(String period);
    
    /**
     * 获取内容类型分布数据
     * @param period 时间周期 (today, week, month, year)
     * @return 内容类型分布数据
     */
    Map<String, Object> getContentTypeDistribution(String period);
} 