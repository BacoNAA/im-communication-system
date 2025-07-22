package com.im.imcommunicationsystem.admin.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 系统通知响应DTO
 * 用于返回给用户端的通知信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemNotificationResponse {
    
    /**
     * 通知ID
     */
    private Long id;
    
    /**
     * 通知标题
     */
    private String title;
    
    /**
     * 通知内容摘要（简短版本）
     */
    private String summary;
    
    /**
     * 通知类型
     */
    private String type;
    
    /**
     * 是否已读
     */
    private Boolean isRead;
    
    /**
     * 发布时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishedAt;
    
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    /**
     * 阅读时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime readAt;
} 