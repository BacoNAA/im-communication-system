package com.im.imcommunicationsystem.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 统计数据响应DTO
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StatisticsResponse {
    
    // 统计数据类型
    private String type;  // users, messages, groups, moments
    
    // 时间段
    private String period;  // today, week, month, year
    
    // 时间标签列表（X轴）
    private List<String> labels;
    
    // 数据列表（Y轴）
    private List<Number> data;
    
    // 对比数据列表（如果需要显示同比数据）
    private List<Number> compareData;
    
    // 总计
    private Long total;
    
    // 增长率
    private Double growthRate;
    
    // 其他统计数据（用于扩展）
    private Map<String, Object> additionalData;
} 