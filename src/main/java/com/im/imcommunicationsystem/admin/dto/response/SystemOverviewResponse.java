package com.im.imcommunicationsystem.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 系统概览响应DTO
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SystemOverviewResponse {
    
    // 用户统计
    private Long totalUsers;
    private Double userGrowth;  // 同比增长率，百分比
    
    // 活跃用户
    private Long activeUsers;
    private Double activeGrowth;  // 同比增长率，百分比
    
    // 消息统计
    private Long totalMessages;
    private Double messageGrowth;  // 同比增长率，百分比
    
    // 群组统计
    private Long totalGroups;
    private Double groupGrowth;  // 同比增长率，百分比
    
    // 动态统计
    private Long totalMoments;
    private Double momentGrowth;  // 同比增长率，百分比
    
    // 新注册用户
    private Long newUsers;
    
    // 时间段
    private String period;  // today, week, month, year
} 