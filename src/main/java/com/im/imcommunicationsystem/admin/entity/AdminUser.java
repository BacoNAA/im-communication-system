package com.im.imcommunicationsystem.admin.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 管理员用户实体类
 */
@Entity
@Table(name = "admin_users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUser {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 50)
    private String username;
    
    @Column(nullable = false)
    private String passwordHash;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AdminRole role;
    
    @Column(columnDefinition = "TEXT")
    private String permissions;
    
    private LocalDateTime lastLoginAt;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive;
    
    /**
     * 管理员角色枚举
     */
    public enum AdminRole {
        SUPER_ADMIN,   // 超级管理员
        ADMIN,         // 普通管理员
        CONTENT_MODERATOR,  // 内容审核员
        SUPPORT        // 客服支持
    }
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (isActive == null) {
            isActive = true;
        }
    }
} 