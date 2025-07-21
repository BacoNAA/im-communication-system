package com.im.imcommunicationsystem.admin.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 用户举报实体类
 */
@Entity
@Table(name = "reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Report {
    
    /**
     * 举报状态枚举
     */
    public enum ReportStatus {
        pending,      // 待处理
        processing,   // 处理中
        resolved,     // 已解决
        rejected      // 已拒绝
    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long reporterId;
    
    private Long reportedUserId;
    
    @Column(length = 50)
    private String reportedContentType;
    
    private Long reportedContentId;
    
    @Column(length = 100)
    private String reason;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status = ReportStatus.pending;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    private LocalDateTime handledAt;
    
    @Column(name = "handler_id")
    private Long handledBy;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = ReportStatus.pending;
        }
    }
} 