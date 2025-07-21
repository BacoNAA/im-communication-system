package com.im.imcommunicationsystem.group.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.Index;

import java.time.LocalDateTime;

/**
 * 群组实体类
 */
@Entity
@Table(name = "`groups`", indexes = {
    @Index(name = "idx_group_owner_id", columnList = "owner_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Group {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "name", nullable = false, length = 100)
    private String name;
    
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;
    
    @Column(name = "avatar_url")
    private String avatarUrl;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "requires_approval", nullable = false)
    private Boolean requiresApproval;
    
    @Column(name = "is_all_muted", nullable = false)
    private Boolean isAllMuted;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    /**
     * 群组是否被封禁
     */
    @Column(name = "is_banned", nullable = false)
    private Boolean isBanned = false;
    
    /**
     * 封禁原因
     */
    @Column(name = "banned_reason", length = 255)
    private String bannedReason;
    
    /**
     * 封禁截止时间
     */
    @Column(name = "banned_until")
    private LocalDateTime bannedUntil;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        
        // Set default values if null
        if (requiresApproval == null) {
            requiresApproval = false;
        }
        
        if (isAllMuted == null) {
            isAllMuted = false;
        }
        
        if (isBanned == null) {
            isBanned = false;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
} 