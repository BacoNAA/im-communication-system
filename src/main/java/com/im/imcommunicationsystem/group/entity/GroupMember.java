package com.im.imcommunicationsystem.group.entity;

import com.im.imcommunicationsystem.group.enums.GroupMemberRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 群成员实体类
 */
@Entity
@Table(name = "group_members")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupMember {
    
    @EmbeddedId
    private GroupMemberId id;
    
    @Column(name = "role", nullable = false)
    @Enumerated(EnumType.STRING)
    private GroupMemberRole role;
    
    @Column(name = "is_muted", nullable = false)
    private Boolean isMuted;
    
    @Column(name = "muted_until")
    private LocalDateTime mutedUntil;
    
    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;
    
    @PrePersist
    protected void onCreate() {
        joinedAt = LocalDateTime.now();
        
        // Set default values if null
        if (isMuted == null) {
            isMuted = false;
        }
        
        if (role == null) {
            role = GroupMemberRole.member;
        }
    }
    
    /**
     * 检查是否是群主
     */
    public boolean isOwner() {
        return role == GroupMemberRole.owner;
    }
    
    /**
     * 检查是否是管理员
     */
    public boolean isAdmin() {
        return role == GroupMemberRole.admin || role == GroupMemberRole.owner;
    }
    
    /**
     * 检查是否被禁言
     */
    public boolean isMuted() {
        if (!Boolean.TRUE.equals(isMuted)) {
            return false;
        }
        
        if (mutedUntil == null) {
            return true;
        }
        
        return mutedUntil.isAfter(LocalDateTime.now());
    }
} 