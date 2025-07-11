package com.im.imcommunicationsystem.relationship.entity;

import com.im.imcommunicationsystem.relationship.enums.ContactRequestStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 好友请求实体类
 * 对应数据库表：friend_requests
 * 管理用户之间的好友请求
 */
@Entity
@Table(name = "friend_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContactRequest {

    /**
     * 请求ID（主键）
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 请求者ID
     */
    @Column(name = "requester_id", nullable = false)
    private Long requesterId;

    /**
     * 接收者ID
     */
    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;

    /**
     * 验证信息
     */
    @Column(name = "message", length = 255)
    private String message;

    /**
     * 请求状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private ContactRequestStatus status = ContactRequestStatus.PENDING;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 处理时间
     */
    @Column(name = "handled_at")
    private LocalDateTime handledAt;
}