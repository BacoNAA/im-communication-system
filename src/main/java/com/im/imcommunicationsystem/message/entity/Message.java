package com.im.imcommunicationsystem.message.entity;

import com.im.imcommunicationsystem.message.enums.MessageStatus;
import com.im.imcommunicationsystem.message.enums.MessageType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 消息实体类
 * 表示系统中的消息信息
 * 
 * @author IM System
 * @version 1.0
 * @since 2024-01-01
 */
@Entity
@Table(name = "messages", indexes = {
        @Index(name = "idx_conversation_created", columnList = "conversation_id, created_at"),
        @Index(name = "idx_sender_created", columnList = "sender_id, created_at"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_message_type", columnList = "message_type"),
        @Index(name = "idx_reply_to", columnList = "reply_to_message_id"),
        @Index(name = "idx_is_read", columnList = "is_read")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    /**
     * 消息ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 会话ID
     */
    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    /**
     * 发送者ID
     */
    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    /**
     * 消息类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private MessageType messageType;

    /**
     * 消息内容
     */
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    /**
     * 媒体文件ID（如果是媒体消息，关联用户模块的FileUpload）
     */
    @Column(name = "media_file_id")
    private Long mediaFileId;

    /**
     * 回复的消息ID
     */
    @Column(name = "reply_to_message_id")
    private Long replyToMessageId;

    /**
     * 转发的原始消息ID
     */
    @Column(name = "original_message_id")
    private Long originalMessageId;

    /**
     * 消息状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private MessageStatus status = MessageStatus.SENT;

    /**
     * 是否已读
     */
    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    /**
     * 获取消息是否已读
     * 
     * @return 是否已读
     */
    public Boolean getIsRead() {
        return isRead != null ? isRead : false;
    }
    
    /**
     * 设置消息是否已读
     * 
     * @param isRead 是否已读
     */
    public void setIsRead(Boolean isRead) {
        this.isRead = isRead != null ? isRead : false;
    }

    /**
     * 是否已编辑
     */
    @Column(name = "edited", nullable = false)
    @Builder.Default
    private Boolean edited = false;

    /**
     * 编辑时间
     */
    @Column(name = "edited_at")
    private LocalDateTime editedAt;

    /**
     * 是否已索引到搜索引擎
     */
    @Column(name = "indexed", nullable = false)
    @Builder.Default
    private Boolean indexed = false;

    /**
     * 消息元数据（JSON格式）
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 删除时间
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * 检查消息是否已删除
     * 
     * @return 是否已删除
     */
    public boolean isDeleted() {
        return status == MessageStatus.DELETED || deletedAt != null;
    }

    /**
     * 检查消息是否已撤回
     * 
     * @return 是否已撤回
     */
    public boolean isRecalled() {
        return status == MessageStatus.RECALLED;
    }

    /**
     * 检查消息是否为回复消息
     * 
     * @return 是否为回复消息
     */
    public boolean isReply() {
        return replyToMessageId != null;
    }

    /**
     * 检查消息是否为转发消息
     * 
     * @return 是否为转发消息
     */
    public boolean isForwarded() {
        return originalMessageId != null;
    }

    /**
     * 检查消息是否包含媒体文件
     * 
     * @return 是否包含媒体文件
     */
    public boolean hasMediaFile() {
        return mediaFileId != null;
    }
}