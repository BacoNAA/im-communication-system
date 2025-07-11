package com.im.imcommunicationsystem.relationship.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 联系人实体类
 * 对应数据库表：contacts
 * 管理用户之间的好友关系
 */
@Entity
@Table(name = "contacts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(ContactId.class)
public class Contact {

    /**
     * 用户ID（复合主键）
     */
    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 好友ID（复合主键）
     */
    @Id
    @Column(name = "friend_id", nullable = false)
    private Long friendId;

    /**
     * 好友备注
     */
    @Column(name = "alias", length = 50)
    private String alias;

    /**
     * 是否屏蔽
     */
    @Column(name = "is_blocked", nullable = false)
    @Builder.Default
    private Boolean isBlocked = false;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}