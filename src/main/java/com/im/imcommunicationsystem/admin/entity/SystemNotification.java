package com.im.imcommunicationsystem.admin.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 系统通知实体类
 */
@Entity
@Table(name = "system_notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemNotification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 100)
    private String title;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    
    @Column(nullable = false, length = 50)
    private String type;
    
    @Column(name = "target_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private TargetType targetType = TargetType.all;
    
    @Column(name = "target_users", columnDefinition = "JSON")
    private String targetUsers;
    
    @Column(name = "is_published", nullable = false)
    private Boolean isPublished = false;
    
    @Column(name = "published_at")
    private LocalDateTime publishedAt;
    
    @Column(name = "created_by", nullable = false)
    private Long createdBy;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    /**
     * 通知目标类型枚举
     */
    public enum TargetType {
        all,            // 所有用户
        specific_users  // 特定用户
    }
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (targetType == null) {
            targetType = TargetType.all;
        }
        if (isPublished == null) {
            isPublished = false;
        }
    }
} 