package com.im.imcommunicationsystem.admin.entity;

import com.im.imcommunicationsystem.admin.enums.AdminRole;
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
    
    /**
     * 管理员角色
     */
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