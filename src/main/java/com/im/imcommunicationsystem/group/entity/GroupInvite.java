package com.im.imcommunicationsystem.group.entity;

import com.im.imcommunicationsystem.group.enums.GroupInviteStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 群组邀请实体类
 */
@Entity
@Table(name = "group_invites")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupInvite {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "group_id", nullable = false)
    private Long groupId;
    
    @Column(name = "inviter_id", nullable = false)
    private Long inviterId;
    
    @Column(name = "invitee_id", nullable = false)
    private Long inviteeId;
    
    @Column(name = "message", length = 255)
    private String message;
    
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private GroupInviteStatus status;
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "handled_at")
    private LocalDateTime handledAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        
        // Set default values if null
        if (status == null) {
            status = GroupInviteStatus.PENDING;
        }
    }
    
    /**
     * 检查邀请是否已过期
     */
    public boolean isExpired() {
        if (expiresAt == null) {
            return false;
        }
        
        return LocalDateTime.now().isAfter(expiresAt);
    }
    
    /**
     * 检查邀请是否待处理
     */
    public boolean isPending() {
        return status == GroupInviteStatus.PENDING && !isExpired();
    }
    
    /**
     * 接受邀请
     */
    public void accept() {
        this.status = GroupInviteStatus.ACCEPTED;
        this.handledAt = LocalDateTime.now();
    }
    
    /**
     * 拒绝邀请
     */
    public void reject() {
        this.status = GroupInviteStatus.REJECTED;
        this.handledAt = LocalDateTime.now();
    }
} 