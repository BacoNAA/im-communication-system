package com.im.imcommunicationsystem.message.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 已读状态实体类
 * 记录用户在会话中已读的最后一条消息ID
 * 
 * @author IM System
 * @version 1.0
 * @since 2024-07-18
 */
@Entity
@Table(name = "read_status", indexes = {
        @Index(name = "idx_read_status_user", columnList = "user_id"),
        @Index(name = "idx_read_status_conversation", columnList = "conversation_id"),
        @Index(name = "idx_read_status_last_read_message", columnList = "last_read_message_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReadStatus {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 会话ID
     */
    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    /**
     * 用户在此对话中已读的最后一条消息ID
     */
    @Column(name = "last_read_message_id", nullable = false)
    private Long lastReadMessageId;

    /**
     * 更新时间
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
} 