package com.im.imcommunicationsystem.relationship.dto.response;

import lombok.Data;

/**
 * 好友请求统计信息响应
 */
@Data
public class ContactRequestStatsResponse {
    
    /**
     * 收到的待处理请求数量
     */
    private long receivedPendingCount;
    
    /**
     * 发送的待处理请求数量
     */
    private long sentPendingCount;
    
    /**
     * 收到的总请求数量
     */
    private long receivedTotalCount;
    
    /**
     * 发送的总请求数量
     */
    private long sentTotalCount;
    
    /**
     * 今日收到的请求数量
     */
    private long todayReceivedCount;
    
    /**
     * 今日发送的请求数量
     */
    private long todaySentCount;
    
    /**
     * 已同意的请求数量（收到的）
     */
    private long acceptedReceivedCount;
    
    /**
     * 已同意的请求数量（发送的）
     */
    private long acceptedSentCount;
}