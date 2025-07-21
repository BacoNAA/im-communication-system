package com.im.imcommunicationsystem.admin.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 管理员操作日志实体类
 */
@Entity
@Table(name = "admin_operation_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminOperationLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long adminId;
    
    @Column(nullable = false, length = 50)
    private String operationType;
    
    @Column(length = 50)
    private String targetType;
    
    private Long targetId;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(length = 45)
    private String ipAddress;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
} 