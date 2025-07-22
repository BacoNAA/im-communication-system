package com.im.imcommunicationsystem.admin.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 系统通知用户状态实体类
 * 记录用户对系统通知的阅读状态
 */
@Entity
@Table(name = "system_notification_status")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemNotificationStatus {
    
    /**
     * 记录ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 系统通知ID
     */
    @Column(name = "notification_id", nullable = false)
    private Long notificationId;
    
    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    /**
     * 是否已读
     */
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;
    
    /**
     * 阅读时间
     */
    @Column(name = "read_at")
    private LocalDateTime readAt;
    
    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
} 