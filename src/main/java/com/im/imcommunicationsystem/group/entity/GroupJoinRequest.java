package com.im.imcommunicationsystem.group.entity;

import com.im.imcommunicationsystem.group.enums.GroupJoinRequestStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 群组加入请求实体类
 * 对应数据库表：group_join_requests
 */
@Entity
@Table(name = "group_join_requests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupJoinRequest {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "group_id", nullable = false)
    private Long groupId;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "message", length = 255)
    private String message;
    
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private GroupJoinRequestStatus status;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "handled_at")
    private LocalDateTime handledAt;
    
    @Column(name = "handler_id")
    private Long handlerId;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        
        // 设置默认值
        if (status == null) {
            status = GroupJoinRequestStatus.PENDING;
        }
    }
    
    /**
     * 检查请求是否待处理
     */
    public boolean isPending() {
        return status == GroupJoinRequestStatus.PENDING;
    }
    
    /**
     * 接受请求
     */
    public void accept(Long handlerId) {
        this.status = GroupJoinRequestStatus.ACCEPTED;
        this.handlerId = handlerId;
        this.handledAt = LocalDateTime.now();
    }
    
    /**
     * 拒绝请求
     */
    public void reject(Long handlerId) {
        this.status = GroupJoinRequestStatus.REJECTED;
        this.handlerId = handlerId;
        this.handledAt = LocalDateTime.now();
    }
    
    /**
     * 取消请求
     */
    public void cancel() {
        this.status = GroupJoinRequestStatus.CANCELLED;
        this.handledAt = LocalDateTime.now();
    }
} 